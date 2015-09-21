sbtPlugin := true

organization := "com.github.kuhnen"

name := "sbt-elasticsearch-plugin"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "org.yaml" % "snakeyaml" % "1.13",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "com.stackmob" %% "newman" % "1.3.5"
)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:postfixOps")

externalDependencyClasspath in Compile ~= (_.filterNot(_.data.toString.contains("commons-logging")))
