// tag::projection[]
package shopping.cart;

import java.util.Optional;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.jdbc.query.javadsl.JdbcReadJournal;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.javadsl.ExactlyOnceProjection;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  // tag::howto-read-side-without-role[]
  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository) {

    ShardedDaemonProcess.get(system)
        .init( // <1>
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionFor(system, transactionManager, repository, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }
  // end::howto-read-side-without-role[]

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          ItemPopularityRepository repository,
          int index) {

    String tag = ShoppingCart.TAGS.get(index); // <2>

    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider = // <3>
        EventSourcedProvider.eventsByTag(
            system,
            JdbcReadJournal.Identifier(), // <4>
            tag);

    return JdbcProjection.exactlyOnce( // <5>
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        () -> new HibernateJdbcSession(transactionManager), // <6>
        () -> new ItemPopularityProjectionHandler(tag, repository), // <7>
        system);
  }
}
// end::projection[]
