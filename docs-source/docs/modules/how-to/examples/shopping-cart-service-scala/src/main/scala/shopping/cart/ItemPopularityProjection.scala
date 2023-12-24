package shopping.cart

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.scaladsl.AtLeastOnceProjection

object ItemPopularityProjection {
  // tag::read-side-with-role[]
  def init(
      system: ActorSystem[_],
      repository: ItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system).withRole("projection"), // <1>
      Some(ProjectionBehavior.Stop))
  }
  // end::read-side-with-role[]

  private def createProjectionFor(
      system: ActorSystem[_],
      repository: ItemPopularityRepository,
      index: Int)
      : AtLeastOnceProjection[Offset, EventEnvelope[ShoppingCart.Event]] = ???
}
