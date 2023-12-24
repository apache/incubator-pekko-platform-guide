package shopping.cart

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.management.scaladsl.PekkoManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

import shopping.order.proto.{ ShoppingOrderService, ShoppingOrderServiceClient }
import org.apache.pekko.grpc.GrpcClientSettings
// tag::ItemPopularityProjection[]
import org.apache.pekko.stream.connectors.cassandra.scaladsl.CassandraSessionRegistry

// end::ItemPopularityProjection[]

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
    try {
      val orderService = orderServiceClient(system)
      init(system, orderService)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_], orderService: ShoppingOrderService): Unit = {
    PekkoManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    // tag::ItemPopularityProjection[]
    val session = CassandraSessionRegistry(system).sessionFor(
      "pekko.persistence.cassandra"
    ) // <1>
    // use same keyspace for the item_popularity table as the offset store
    val itemPopularityKeyspace =
      system.settings.config
        .getString("pekko.projection.cassandra.offset-store.keyspace")
    val itemPopularityRepository =
      new ItemPopularityRepositoryImpl(session, itemPopularityKeyspace)(
        system.executionContext
      ) // <2>

    ItemPopularityProjection.init(system, itemPopularityRepository) // <3>
    // end::ItemPopularityProjection[]

    PublishEventsProjection.init(system)

    SendOrderProjection.init(system, orderService)

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService =
      new ShoppingCartServiceImpl(system, itemPopularityRepository)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)
  }

  protected def orderServiceClient(
      system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceClientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString("shopping-order-service.host"),
          system.settings.config.getInt("shopping-order-service.port"))(system)
        .withTls(false)
    ShoppingOrderServiceClient(orderServiceClientSettings)(system)
  }

}
