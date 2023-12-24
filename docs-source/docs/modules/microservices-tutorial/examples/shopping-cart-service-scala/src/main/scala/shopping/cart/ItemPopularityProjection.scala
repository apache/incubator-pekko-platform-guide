// tag::projection[]
package shopping.cart

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.jdbc.scaladsl.JdbcProjection
import org.apache.pekko.projection.scaladsl.{
  ExactlyOnceProjection,
  SourceProvider
}
import org.apache.pekko.projection.{ ProjectionBehavior, ProjectionId }
import shopping.cart.repository.{ ItemPopularityRepository, ScalikeJdbcSession }

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
      : ExactlyOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index) // <2>

    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] = // <3>
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier, // <4>
        tag = tag)

    JdbcProjection.exactlyOnce( // <5>
      projectionId = ProjectionId("ItemPopularityProjection", tag),
      sourceProvider,
      handler = () =>
        new ItemPopularityProjectionHandler(tag, system, repository), // <6>
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }

}
// end::projection[]
