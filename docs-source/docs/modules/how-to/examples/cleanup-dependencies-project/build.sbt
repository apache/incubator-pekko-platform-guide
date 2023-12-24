name := "cleanup-dependencies-project"

organization := "org.apache.pekko.samples"
organizationHomepage := Some(url("https://pekko.apache.org/"))
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

// tag::remove-pekko-persistence-cassandra-version[]
val PekkoPersistenceCassandraVersion = "1.0.0"
// end::remove-pekko-persistence-cassandra-version[]
// tag::add-pekko-persistence-jdbc-version[]
val PekkoPersistenceJdbcVersion = "1.0.0"
// end::add-pekko-persistence-jdbc-version[]
// tag::remove-pekko-connectors-kafka-version[]
val PekkoConnectorsKafkaVersion = "1.0.0"
// end::remove-pekko-connectors-kafka-version[]
// tag::remove-pekko-projection-version[]
val PekkoProjectionVersion = "1.0.0"
// end::remove-pekko-projection-version[]

// tag::remove-grpc-plugin[]
enablePlugins(PekkoGrpcPlugin)
// end::remove-grpc-plugin[]

enablePlugins(JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  // tag::remove-pekko-persistence-cassandra[]
  "org.apache.pekko" %% "pekko-persistence-cassandra" % PekkoPersistenceCassandraVersion,
  // end::remove-pekko-persistence-cassandra[]

  // tag::add-pekko-persistence-jdbc[]
  "org.apache.pekko" %% "pekko-persistence-jdbc" % PekkoPersistenceJdbcVersion,
  // end::add-pekko-persistence-jdbc[]

  // tag::remove-pekko-projection[]
  "org.apache.pekko" %% "pekko-projection-eventsourced" % PekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-projection-cassandra" % PekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-projection-jdbc" % PekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
  "org.apache.pekko" %% "pekko-projection-testkit" % PekkoProjectionVersion % Test,
  // end::remove-pekko-projection[]

  /*
  // tag::replace-offset-store-for-projections-jdbc[]
  -  "org.apache.pekko" %% "pekko-projection-cassandra" % PekkoProjectionVersion,
  +  "org.apache.pekko" %% "pekko-projection-jdbc" % PekkoProjectionVersion,
  // end::replace-offset-store-for-projections-jdbc[]
   */
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-management" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-http" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery-kubernetes-api" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-persistence-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion,
  "org.apache.pekko" %% "pekko-discovery" % PekkoVersion,
  // tag::remove-pekko-connectors-kafka[]
  "org.apache.pekko" %% "pekko-connectors-kafka" % PekkoConnectorsKafkaVersion,
  // end::remove-pekko-connectors-kafka[]

  // Logging
  "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.13",
  // Test dependencies
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-persistence-testkit" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test)
