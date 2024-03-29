package shopping.cart;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.MemberStatus;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.Join;
import org.apache.pekko.persistence.testkit.javadsl.PersistenceInit;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSession;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSessionRegistry;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ItemPopularityIntegrationTest {
  private static final long UNIQUE_QUALIFIER = System.currentTimeMillis();
  private static final String KEYSPACE = "ItemPopularityIntegrationTest_" + UNIQUE_QUALIFIER;

  private static Config config() {
    return ConfigFactory.parseString(
            "pekko.persistence.cassandra.journal.keyspace = "
                + KEYSPACE
                + "\n"
                + "pekko.persistence.cassandra.snapshot.keyspace = "
                + KEYSPACE
                + "\n"
                + "pekko.projection.cassandra.offset-store.keyspace = "
                + KEYSPACE
                + "\n")
        .withFallback(ConfigFactory.load("item-popularity-integration-test.conf"));
  }

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource(config());

  private static ActorSystem<?> system = testKit.system();
  private static ItemPopularityRepository itemPopularityRepository;

  @BeforeClass
  public static void beforeClass() throws Exception {
    // avoid concurrent creation of keyspace and tables
    PersistenceInit.initializeDefaultPlugins(system, Duration.ofSeconds(10))
        .toCompletableFuture()
        .get(10, SECONDS);
    CreateTableTestUtils.createTables(system);

    // use same keyspace for the item_popularity table as the offset store
    CassandraSession session =
        CassandraSessionRegistry.get(system).sessionFor("pekko.persistence.cassandra");
    String itemPopularityKeyspace =
        system.settings().config().getString("pekko.projection.cassandra.offset-store.keyspace");

    ShoppingCart.init(system);

    itemPopularityRepository = new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace);
    ItemPopularityProjection.init(system, itemPopularityRepository);

    // form a single node cluster and make sure that completes before running the test
    Cluster node = Cluster.get(system);
    node.manager().tell(Join.create(node.selfMember().address()));

    // let the node join and become Up
    TestProbe<Object> probe = testKit.createTestProbe();
    probe.awaitAssert(
        () -> {
          assertEquals(MemberStatus.up(), node.selfMember().status());
          return null;
        });
  }

  @Test
  public void consumeCartEventsAndUpdatePopularityCount() throws Exception {
    ClusterSharding sharding = ClusterSharding.get(system);

    final String cartId1 = "cart1";
    final String cartId2 = "cart2";
    final String item1 = "item1";
    final String item2 = "item2";

    EntityRef<ShoppingCart.Command> cart1 = sharding.entityRefFor(ShoppingCart.ENTITY_KEY, cartId1);
    EntityRef<ShoppingCart.Command> cart2 = sharding.entityRefFor(ShoppingCart.ENTITY_KEY, cartId2);

    final Duration timeout = Duration.ofSeconds(3);

    CompletionStage<ShoppingCart.Summary> reply1 =
        cart1.askWithStatus(replyTo -> new ShoppingCart.AddItem(item1, 3, replyTo), timeout);
    ShoppingCart.Summary summary1 = reply1.toCompletableFuture().get(3, SECONDS);
    assertEquals(3, summary1.items.get(item1).intValue());

    TestProbe<Object> probe = testKit.createTestProbe();
    probe.awaitAssert(
        () -> {
          Optional<Long> item1Popularity =
              itemPopularityRepository.getItem(item1).toCompletableFuture().get(3, SECONDS);
          assertTrue(item1Popularity.isPresent());
          assertEquals(3L, item1Popularity.get().intValue());
          return null;
        });

    CompletionStage<ShoppingCart.Summary> reply2 =
        cart1.askWithStatus(replyTo -> new ShoppingCart.AddItem(item2, 5, replyTo), timeout);
    ShoppingCart.Summary summary2 = reply2.toCompletableFuture().get(3, SECONDS);
    assertEquals(2, summary2.items.size());
    assertEquals(5, summary2.items.get(item2).intValue());
    // another cart
    CompletionStage<ShoppingCart.Summary> reply3 =
        cart2.askWithStatus(replyTo -> new ShoppingCart.AddItem(item2, 4, replyTo), timeout);
    ShoppingCart.Summary summary3 = reply3.toCompletableFuture().get(3, SECONDS);
    assertEquals(1, summary3.items.size());
    assertEquals(4L, summary3.items.get(item2).intValue());

    probe.awaitAssert(
        () -> {
          Optional<Long> item2Popularity =
              itemPopularityRepository.getItem(item2).toCompletableFuture().get(3, SECONDS);
          assertTrue(item2Popularity.isPresent());
          assertEquals(5 + 4, item2Popularity.get().longValue());
          return null;
        });
  }
}
