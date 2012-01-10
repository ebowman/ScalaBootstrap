name := "scala-streamer"

version := "0.0.1"

scalaVersion := "2.9.1"

resolvers ++= Seq(
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Akka Repository" at "http://akka.io/repository/",
        "java.net repo" at "http://download.java.net/maven/2/"
        )

libraryDependencies ++= Seq(
        "cc.spray.can" % "spray-can" % "0.9.1",
        "ch.qos.logback" % "logback-classic" % "0.9.28",
        "ch.qos.logback" % "logback-core" % "0.9.28",
        "org.scalatest" %% "scalatest" % "1.6.1" % "test",
        "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test"
        )

