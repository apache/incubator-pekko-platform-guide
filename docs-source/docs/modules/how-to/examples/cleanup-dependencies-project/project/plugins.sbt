// tag::remove-grpc-plugin[]
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.0.1")
// end::remove-grpc-plugin[]

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
