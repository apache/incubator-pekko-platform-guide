// tag::projection[]
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

public final class ItemPopularityProjection {

  private ItemPopularityProjection() {}

  public static void init(ActorSystem<?> system, ItemPopularityRepository repository) {
    ShardedDaemonProcess.get(system)
        .init( // <1>
            ProjectionBehavior.Command.class,
            "ItemPopularityProjection",
            ShoppingCart.TAGS.size(),
            index -> ProjectionBehavior.create(createProjectionFor(system, repository, index)),
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage()));
  }

  private static AtLeastOnceProjection<Offset, EventEnvelope<ShoppingCart.Event>>
      createProjectionFor(ActorSystem<?> system, ItemPopularityRepository repository, int index) {
    String tag = ShoppingCart.TAGS.get(index); // <2>

    SourceProvider<Offset, EventEnvelope<ShoppingCart.Event>> sourceProvider = // <3>
        EventSourcedProvider.eventsByTag(
            system,
            CassandraReadJournal.Identifier(), // <4>
            tag);

    return CassandraProjection.atLeastOnce( // <5>
        ProjectionId.of("ItemPopularityProjection", tag),
        sourceProvider,
        () -> new ItemPopularityProjectionHandler(tag, repository)); // <6>
  }
}
// end::projection[]
