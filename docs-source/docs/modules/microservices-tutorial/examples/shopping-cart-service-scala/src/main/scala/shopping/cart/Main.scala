package shopping.cart

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.management.scaladsl.PekkoManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

import org.apache.pekko.actor.CoordinatedShutdown
import shopping.cart.repository.ItemPopularityRepositoryImpl
import shopping.cart.repository.ScalikeJdbcSetup
// tag::SendOrderProjection[]
import shopping.order.proto.{ ShoppingOrderService, ShoppingOrderServiceClient }
import org.apache.pekko.grpc.GrpcClientSettings

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
    // end::SendOrderProjection[]
    ScalikeJdbcSetup.init(system)

    PekkoManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    val itemPopularityRepository = new ItemPopularityRepositoryImpl()
    ItemPopularityProjection.init(system, itemPopularityRepository)

    PublishEventsProjection.init(system)

    // tag::SendOrderProjection[]
    SendOrderProjection.init(system, orderService) // <1>
    // end::SendOrderProjection[]

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService =
      new ShoppingCartServiceImpl(system, itemPopularityRepository)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)
    // tag::SendOrderProjection[]
  }

  protected def orderServiceClient( // <2>
      system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceClientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString("shopping-order-service.host"),
          system.settings.config.getInt("shopping-order-service.port"))(system)
        .withTls(false)
    ShoppingOrderServiceClient(orderServiceClientSettings)(system)
  }
  // end::SendOrderProjection[]

}
