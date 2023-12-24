package shopping.cart

import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.SendProducer
import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.{
  AtLeastOnceProjection,
  SourceProvider
}
import org.apache.pekko.projection.{ ProjectionBehavior, ProjectionId }
import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}

object PublishEventsProjection {

  def init(system: ActorSystem[_]): Unit = {
    val sendProducer = createProducer(system)
    val topic =
      system.settings.config.getString("shopping-cart-service.kafka.topic")

    ShardedDaemonProcess(system).init(
      name = "PublishEventsProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(
          createProjectionFor(system, topic, sendProducer, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProducer(
      system: ActorSystem[_]): SendProducer[String, Array[Byte]] = {
    val producerSettings =
      ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    val sendProducer =
      SendProducer(producerSettings)(system)
    CoordinatedShutdown(system).addTask(
      CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
      "close-sendProducer") { () =>
      sendProducer.close()
    }
    sendProducer
  }

  private def createProjectionFor(
      system: ActorSystem[_],
      topic: String,
      sendProducer: SendProducer[String, Array[Byte]],
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
      projectionId = ProjectionId("PublishEventsProjection", tag),
      sourceProvider,
      handler =
        () => new PublishEventsProjectionHandler(system, topic, sendProducer))
  }

}
