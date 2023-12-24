package shopping.order;

import com.typesafe.config.Config;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.order.proto.ShoppingOrderService;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingOrderService");
    try {
      init(system);
    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system) {
    PekkoManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-order-service.grpc.interface");
    int grpcPort = config.getInt("shopping-order-service.grpc.port");
    ShoppingOrderService grpcService = new ShoppingOrderServiceImpl();
    ShoppingOrderServer.start(grpcInterface, grpcPort, system, grpcService);
  }
}
