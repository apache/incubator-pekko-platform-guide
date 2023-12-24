package shopping.cart;

import com.typesafe.config.Config;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
// tag::SendOrderProjection[]
import org.apache.pekko.grpc.GrpcClientSettings;
// end::SendOrderProjection[]
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.ItemPopularityRepository;
import shopping.cart.repository.SpringIntegration;
// tag::SendOrderProjection[]
import shopping.order.proto.ShoppingOrderService;
import shopping.order.proto.ShoppingOrderServiceClient;

public class Main {
  // end::SendOrderProjection[]

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // tag::SendOrderProjection[]
  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    try {
      init(system, orderServiceClient(system));
    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system, ShoppingOrderService orderService) {
    // end::SendOrderProjection[]
    PekkoManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ShoppingCart.init(system);

    ApplicationContext springContext = SpringIntegration.applicationContext(system);

    ItemPopularityRepository itemPopularityRepository =
        springContext.getBean(ItemPopularityRepository.class);

    JpaTransactionManager transactionManager = springContext.getBean(JpaTransactionManager.class);

    ItemPopularityProjection.init(system, transactionManager, itemPopularityRepository);

    PublishEventsProjection.init(system, transactionManager);

    // tag::SendOrderProjection[]
    SendOrderProjection.init(system, transactionManager, orderService); // <1>
    // end::SendOrderProjection[]

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    int grpcPort = config.getInt("shopping-cart-service.grpc.port");
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, itemPopularityRepository);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);
    // tag::SendOrderProjection[]
  }

  static ShoppingOrderService orderServiceClient(ActorSystem<?> system) { // <2>
    GrpcClientSettings orderServiceClientSettings =
        GrpcClientSettings.connectToServiceAt(
                system.settings().config().getString("shopping-order-service.host"),
                system.settings().config().getInt("shopping-order-service.port"),
                system)
            .withTls(false);

    return ShoppingOrderServiceClient.create(orderServiceClientSettings, system);
  }
  // end::SendOrderProjection[]

}
