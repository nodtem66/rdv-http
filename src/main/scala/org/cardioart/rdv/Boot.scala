package org.cardioart.rdv

import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.{ConfigFactory, Config}
import akka.io.IO
import org.cardioart.rdv.actor._
import spray.can.Http

/**
 * This file is a startup program for HTTP server
 * The configs are passed into this program via command-line arguments
 */
object Boot extends App {

  val config: Config = ConfigFactory.load()
  implicit val system = ActorSystem("rdv-http", config)

  // actors
  val statActor = system.actorOf(Props[Stat], "stat-actor")
  val connectionActor = system.actorOf(ConnectionSupervisor.props[Connection](), "connection-supervisor")
  val sessionActor = system.actorOf(SessionSupervisor.props[Session](), "session-supervisor")

  // HTTP Restful API interface
  val apiActor = system.actorOf(Props(new Api(connectionActor, sessionActor)), "api-actor")
  IO(Http) ! Http.Bind(apiActor, interface = "localhost", port = 8080)
}
