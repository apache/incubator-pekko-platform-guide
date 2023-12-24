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
import shopping.order.proto.ShoppingOrderService

object SendOrderProjection {

  def init(system: ActorSystem[_], orderService: ShoppingOrderService): Unit = {
    ShardedDaemonProcess(system).init(
      name = "SendOrderProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, orderService, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProjectionFor(
      system: ActorSystem[_],
      orderService: ShoppingOrderService,
      index: Int): AtLeastOnceProjection[
    Offset,
    EventEnvelope[ShoppingCart.Event]] = { // <1>
    val tag = ShoppingCart.tags(index)
    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier, // <2>
        tag = tag)

    CassandraProjection.atLeastOnce( // <3>
      projectionId = ProjectionId("SendOrderProjection", tag),
      sourceProvider,
      handler = () => new SendOrderProjectionHandler(system, orderService))
  }

}
