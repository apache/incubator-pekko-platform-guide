name := "shopping-cart-service"

organization := "org.apache.pekko.samples"
organizationHomepage := Some(url("https://pekko.apache.org"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "2.13.12"

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := false
Global / cancelable := false // ctrl-c

val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.0"
val PekkoManagementVersion = "1.0.0"
// tag::pekko-persistence-cassandra[]
val PekkoPersistenceCassandraVersion = "1.0.0"

// end::pekko-persistence-cassandra[]
val PekkoConnectorsKafkaVersion = "1.0.0"
val PekkoProjectionVersion = "1.0.0"

enablePlugins(PekkoGrpcPlugin)

enablePlugins(JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

// tag::pekko-persistence-cassandra[]
libraryDependencies ++= Seq(
  // end::pekko-persistence-cassandra[]
  // 1. Basic dependencies for a clustered application
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
  // Pekko Management powers Health Checks and Pekko Cluster Bootstrapping
  "org.apache.pekko" %% "pekko-management" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-management-cluster-http" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery-kubernetes-api" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery" % PekkoVersion,
  // Common dependencies for logging and testing
  "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.13",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  // 3. Using Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion,
  // tag::pekko-persistence-cassandra[]
  "org.apache.pekko" %% "pekko-persistence-cassandra" % PekkoPersistenceCassandraVersion,
  // end::pekko-persistence-cassandra[]
  "org.apache.pekko" %% "pekko-persistence-testkit" % PekkoVersion % Test,
  // 4. Querying or projecting data from Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
  "org.apache.pekko" %% "pekko-projection-eventsourced" % PekkoProjectionVersion,
  // tag::pekko-projection-cassandra[]
  "org.apache.pekko" %% "pekko-projection-cassandra" % PekkoProjectionVersion,
  // end::pekko-projection-cassandra[]
  "org.apache.pekko" %% "pekko-connectors-kafka" % PekkoConnectorsKafkaVersion,
  "org.apache.pekko" %% "pekko-projection-testkit" % PekkoProjectionVersion % Test
  // tag::pekko-persistence-cassandra[]
)
// end::pekko-persistence-cassandra[]
