package shopping.cart;

import java.util.Optional;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.cassandra.query.javadsl.CassandraReadJournal;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.cassandra.javadsl.CassandraProjection;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.javadsl.AtLeastOnceProjection;
import org.apache.pekko.projection.javadsl.SourceProvider;
import shopping.order.proto.ShoppingOrderService;

public class SendOrderProjection {

  private SendOrderProjection() {}

  public static void init(ActorSystem<?> system, ShoppingOrderService orderService) {
    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "SendOrderProjection",
            ShoppingCart.TAGS.size(),
            index -> ProjectionBehavior.create(createProjectionsFor(system, orderService, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>> // <1>
      createProjectionsFor(ActorSystem<?> system, ShoppingOrderService orderService, int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, CassandraReadJournal.Identifier(), tag); // <2>

    return CassandraProjection.atLeastOnce( // <3>
        ProjectionId.of("SendOrderProjection", tag),
        sourceProvider,
        () -> new SendOrderProjectionHandler(system, orderService));
  }
}
