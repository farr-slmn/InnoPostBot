val ScalatraVersion = "2.6.3"

organization := "ru.innopolis.university"

name := "InnoPostBot"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.8.v20171121" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "info.mukel" %% "telegrambot4s" % "3.0.14",
  "net.debasishg" %% "redisclient" % "3.5",
  "org.tpolecat" %% "doobie-core" % "0.5.2",
  "org.tpolecat" %% "doobie-postgres"  % "0.5.2"
)


enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
