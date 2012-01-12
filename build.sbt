name := "scala-shoutcast"

version := "0.0.1"

scalaVersion := "2.9.1"

resolvers ++= Seq(
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Akka Repository" at "http://akka.io/repository/",
        "java.net repo" at "http://download.java.net/maven/2/"
        )

libraryDependencies ++= Seq(
        "de.huxhorn.sulky" % "de.huxhorn.sulky.3rdparty.jlayer" % "1.0",
        "se.scalablesolutions.akka" % "akka-actor" % "1.3-RC6",
        "ch.qos.logback" % "logback-classic" % "0.9.28",
        "ch.qos.logback" % "logback-core" % "0.9.28",
        "org.scalatest" %% "scalatest" % "1.6.1" % "test",
        "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test"
        )

