name := "ScalaBootstrap"

version := "0.0.1"

scalaVersion := "2.9.1"

resolvers ++= Seq(
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Akka Repository" at "http://akka.io/repository/",
    "java.net repo" at "http://download.java.net/maven/2/"
)

libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-actor" % "2.0-M2",
    "com.typesafe.akka" % "akka-remote" % "2.0-M2",
    "com.typesafe.akka" % "akka-kernel" % "2.0-M2"
)

