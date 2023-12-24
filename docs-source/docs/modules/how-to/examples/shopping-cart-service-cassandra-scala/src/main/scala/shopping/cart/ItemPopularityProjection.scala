// tag::projection[]
package shopping.cart

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.projection.ProjectionId
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.AtLeastOnceProjection
import org.apache.pekko.projection.scaladsl.SourceProvider

object ItemPopularityProjection {
  // tag::howto-read-side-without-role[]
  def init(
      system: ActorSystem[_],
      repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init( // <1>
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }
  // end::howto-read-side-without-role[]

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int)
      : AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index) // <2>

    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] = // <3>
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier, // <4>
        tag = tag)

    CassandraProjection.atLeastOnce( // <5>
      projectionId = ProjectionId("ItemPopularityProjection", tag),
      sourceProvider,
      handler = () =>
        new ItemPopularityProjectionHandler(tag, system, repository) // <6>
    )
  }

}
// end::projection[]
