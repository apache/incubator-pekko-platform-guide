package shopping.cart;

import java.util.Optional;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.javadsl.ExactlyOnceProjection;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.ItemPopularityRepository;

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  // tag::read-side-with-role[]
  public static void init(
      ActorSystem<?> system,
      JpaTransactionManager transactionManager,
      ItemPopularityRepository repository) {

    ShardedDaemonProcess.get(system)
        .init(
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index ->
                ProjectionBehavior.create(
                    createProjectionFor(system, transactionManager, repository, index)),
            ShardedDaemonProcessSettings.create(system).withRole("projection"), // <1>
            Optional.of(ProjectionBehavior.stopMessage()));
  }
  // end::read-side-with-role[]

  private static ExactlyOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(
          ActorSystem<?> system,
          JpaTransactionManager transactionManager,
          ItemPopularityRepository repository,
          int index) {

    return null; // not implemented here
  }
}
