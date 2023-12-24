package shopping.cart;

import java.util.Optional;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.kafka.ProducerSettings;
import org.apache.pekko.kafka.javadsl.SendProducer;
import org.apache.pekko.persistence.cassandra.query.javadsl.CassandraReadJournal;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.cassandra.javadsl.CassandraProjection;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.javadsl.AtLeastOnceProjection;
import org.apache.pekko.projection.javadsl.SourceProvider;

public final class PublishEventsProjection {

  private PublishEventsProjection() {}

  public static void init(ActorSystem<?> system) {
    SendProducer<String, byte[]> sendProducer = createProducer(system);
    String topic = system.settings().config().getString("shopping-cart-service.kafka.topic");

    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "PublishEventsProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(createProjectionFor(system, topic, sendProducer, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static SendProducer<String, byte[]> createProducer(ActorSystem<?> system) {
    ProducerSettings<String, byte[]> producerSettings =
        ProducerSettings.create(system, new StringSerializer(), new ByteArraySerializer());
    SendProducer<String, byte[]> sendProducer = new SendProducer<>(producerSettings, system);
    CoordinatedShutdown.get(system)
        .addTask(
            CoordinatedShutdown.PhaseActorSystemTerminate(),
            "close-sendProducer",
            () -> sendProducer.close());
    return sendProducer;
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>> // <1>
      createProjectionFor(
      ActorSystem<?> system, String topic, SendProducer<String, byte[]> sendProducer, int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, CassandraReadJournal.Identifier(), tag); // <2>

    return CassandraProjection.atLeastOnce( // <3>
        ProjectionId.of("PublishEventsProjection", tag),
        sourceProvider,
        () -> new PublishEventsProjectionHandler(topic, sendProducer));
  }
}
