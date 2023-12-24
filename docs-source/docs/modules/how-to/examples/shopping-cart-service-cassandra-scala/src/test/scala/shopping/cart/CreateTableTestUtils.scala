package shopping.cart

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.connectors.cassandra.scaladsl.CassandraSessionRegistry
import shopping.cart.ItemPopularityRepositoryImpl.popularityTable

object CreateTableTestUtils {

  def createTables(system: ActorSystem[_]): Unit = {
    import org.slf4j.LoggerFactory
    import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
    import scala.concurrent.Await
    import scala.concurrent.duration._

    // ok to block here, main thread
    Await.result(
      CassandraProjection.createOffsetTableIfNotExists()(system),
      30.seconds)

    // use same keyspace for the item_popularity table as the offset store
    val keyspace = system.settings.config
      .getString("pekko.projection.cassandra.offset-store.keyspace")
    val session =
      CassandraSessionRegistry(system).sessionFor("pekko.persistence.cassandra")

    Await.result(
      session.executeDDL(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.$popularityTable (
      item_id text,
      count counter,
      PRIMARY KEY (item_id))
      """),
      30.seconds)

    LoggerFactory
      .getLogger("shopping.cart.CreateTableTestUtils")
      .info("Created keyspace [{}] and tables", keyspace)
  }
}
