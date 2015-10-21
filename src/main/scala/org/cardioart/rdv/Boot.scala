package org.cardioart.rdv

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{ConfigFactory, Config}
import akka.io.IO
import spray.can.Http

/**
 * This file is a startup program for HTTP server
 * The configs are passed into this program via command-line arguments
 */
object Boot extends App {
  val config: Config = ConfigFactory.load()
  implicit val system = ActorSystem("rdv-http", config)
  val apiActor = system.actorOf(Props[ApiActor], "api-actor")
  IO(Http) ! Http.Bind(apiActor, interface = "localhost", port = 8080)
}
