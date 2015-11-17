package org.cardioart.rdv

import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.{ConfigFactory, Config}
import akka.io.IO
import org.cardioart.rdv.actor._
import scopt.OptionParser
import spray.can.Http

/**
 * This is a arguments configuration
 * see: https://github.com/scopt/scopt
 */
case class OptionConfig(port: Int = 8080)
/**
 * This file is a startup program for HTTP server
 * The configs are passed into this program via command-line arguments
 */
object Boot extends App {

  val parser = new OptionParser[OptionConfig]("rdv-http") {
    head("rdv-http", "0.1.1")
    opt[Int]('p', "port") valueName("<port>") action { (x,c) =>
      c.copy(port = x) } validate { x =>
      if (x > 0) success else failure("port number must more than zero") } text("port number is required")
    help("help") text("print usage text")
  }

  // parse the program arguments
  parser.parse(args, OptionConfig()) match {
    case Some(option) =>

      val config: Config = ConfigFactory.load()
      implicit val system = ActorSystem("rdv-http", config)

      // actors
      val statActor = system.actorOf(Props[Stat], "stat-actor")
      val connectionActor = system.actorOf(ConnectionSupervisor.props[Connection](), "connection-supervisor")
      val sessionActor = system.actorOf(SessionSupervisor.props[Session](), "session-supervisor")

      // HTTP Restful API interface
      val apiActor = system.actorOf(Props(new Api(connectionActor, sessionActor)), "api-actor")
      IO(Http) ! Http.Bind(apiActor, interface = "::0", port = option.port)

    case None =>
      println("arguments is not valid. use --help to see usage text")
      System.exit(0)
  }
}
