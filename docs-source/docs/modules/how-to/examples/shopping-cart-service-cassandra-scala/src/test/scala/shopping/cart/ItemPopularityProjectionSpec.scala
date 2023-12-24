package shopping.cart

import java.time.Instant

import scala.concurrent.Future

import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.ProjectionId
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.testkit.scaladsl.TestProjection
import org.apache.pekko.projection.testkit.scaladsl.TestSourceProvider
import org.apache.pekko.projection.testkit.scaladsl.ProjectionTestKit
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.wordspec.AnyWordSpecLike

object ItemPopularityProjectionSpec {
  // stub out the db layer and simulate recording item count updates
  class TestItemPopularityRepository extends ItemPopularityRepository {
    var counts: Map[String, Long] = Map.empty

    override def update(itemId: String, delta: Int): Future[Done] =
      Future.successful {
        counts = counts + (itemId -> (counts.getOrElse(itemId, 0L) + delta))
        Done
      }

    override def getItem(itemId: String): Future[Option[Long]] =
      Future.successful(counts.get(itemId))
  }
}

class ItemPopularityProjectionSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import ItemPopularityProjectionSpec.TestItemPopularityRepository

  private val projectionTestKit = ProjectionTestKit(system)

  private def createEnvelope(
      event: ShoppingCart.Event,
      seqNo: Long,
      timestamp: Long = 0L) =
    EventEnvelope(
      Offset.sequence(seqNo),
      "persistenceId",
      seqNo,
      event,
      timestamp)

  "The events from the Shopping Cart" should {

    "update item popularity counts by the projection" in {

      val events =
        Source(
          List[EventEnvelope[ShoppingCart.Event]](
            createEnvelope(
              ShoppingCart.ItemAdded("a7098", "bowling shoes", 1),
              0L),
            createEnvelope(
              ShoppingCart.ItemQuantityAdjusted("a7098", "bowling shoes", 2, 1),
              1L),
            createEnvelope(
              ShoppingCart
                .CheckedOut("a7098", Instant.parse("2020-01-01T12:00:00.00Z")),
              2L),
            createEnvelope(
              ShoppingCart.ItemAdded("0d12d", "pekko t-shirt", 1),
              3L),
            createEnvelope(ShoppingCart.ItemAdded("0d12d", "skis", 1), 4L),
            createEnvelope(ShoppingCart.ItemRemoved("0d12d", "skis", 1), 5L),
            createEnvelope(
              ShoppingCart
                .CheckedOut("0d12d", Instant.parse("2020-01-01T12:05:00.00Z")),
              6L)))

      val repository = new TestItemPopularityRepository
      val projectionId =
        ProjectionId("item-popularity", "carts-0")
      val sourceProvider =
        TestSourceProvider[Offset, EventEnvelope[ShoppingCart.Event]](
          events,
          extractOffset = env => env.offset)
      val projection =
        TestProjection[Offset, EventEnvelope[ShoppingCart.Event]](
          projectionId,
          sourceProvider,
          () =>
            new ItemPopularityProjectionHandler("carts-0", system, repository))

      projectionTestKit.run(projection) {
        repository.counts shouldBe Map(
          "bowling shoes" -> 2,
          "pekko t-shirt" -> 1,
          "skis" -> 0)
      }
    }
  }

}
