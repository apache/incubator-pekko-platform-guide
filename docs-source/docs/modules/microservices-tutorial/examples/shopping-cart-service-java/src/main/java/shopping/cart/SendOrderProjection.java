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
import org.apache.pekko.projection.javadsl.AtLeastOnceProjection;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.order.proto.ShoppingOrderService;

public class SendOrderProjection {

  private SendOrderProjection() {}

  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ShoppingOrderService orderService) {
    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "SendOrderProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionsFor(system, transactionManager, orderService, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionsFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          ShoppingOrderService orderService,
          int index) {
    String tag = ShoppingCart.TAGS.get(index);
    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider =
        EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

    return JdbcProjection.atLeastOnceAsync(
        ProjectionId.of("SendOrderProjection", tag),
        sourceProvider,
        () -> new HibernateJdbcSession(transactionManager),
        () -> new SendOrderProjectionHandler(system, orderService),
        system);
  }
}
