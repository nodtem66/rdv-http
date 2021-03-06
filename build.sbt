organization := "org.cardioart"

name := "rdv-http"

version := "0.1.1"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.4.0"
  val sprayVersion = "1.3.3"
  Seq(
    "org.scala-lang" % "scala-reflect" % "2.11.7",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
    "com.github.scopt" %% "scopt" % "3.3.0",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )
}

resolvers += "spray repo" at "http://repo.spray.io"
resolvers += Resolver.sonatypeRepo("public")

// for assembly plugins
assemblyJarName in assembly := "rdv-http.jar"

test in assembly := {}