name := "neo4j-rdf-plugin"

version := "1.0"

scalaVersion := "2.10.5"

val Neo4jVersion = "2.1.6"
val BlueprintsVersion = "2.7.0-SNAPSHOT"
val SesameVersion = "2.7.13"

libraryDependencies ++= Seq(
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0" % "provided",
  "org.neo4j" % "neo4j" % Neo4jVersion % "provided",
  "com.tinkerpop.blueprints" % "blueprints-sail-graph" % BlueprintsVersion,
  "com.tinkerpop.blueprints" % "blueprints-graph-sail" % BlueprintsVersion,
  "com.tinkerpop.blueprints" % "blueprints-neo4j2-graph" % BlueprintsVersion,
  "org.openrdf.sesame" % "sesame-repository-sail" % SesameVersion,
  "com.sun.jersey" % "jersey-core" % "1.9" % "provided",
  //--- test dependencies
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.11" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.neo4j.app" % "neo4j-server" % Neo4jVersion % "test" classifier "tests",
  "org.neo4j" % "neo4j-kernel" % Neo4jVersion % "test" classifier "tests",
  "org.neo4j.test" % "neo4j-harness" % Neo4jVersion % "test"
).map(_.exclude("org.slf4j", "slf4j-log4j12"))

resolvers in ThisBuild ++= Seq(
  Resolver.mavenLocal
)

net.virtualvoid.sbt.graph.Plugin.graphSettings