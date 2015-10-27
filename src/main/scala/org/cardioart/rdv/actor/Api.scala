package org.cardioart.rdv.actor

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.{ask,pipe}
import akka.util.Timeout
import org.cardioart.rdv.actor.ConnectionSupervisor.QueryConnection
import org.cardioart.rdv.parser.StatsJsonProtocol._
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpServiceActor
import spray.routing.PathMatchers.Segment

import scala.concurrent.duration._

class Api(connectionSupervisorRef: ActorRef, sessionSupervisorRef: ActorRef)
  extends HttpServiceActor with ActorLogging {

  implicit val timeout: Timeout = 1.second
  import context.dispatcher

  def receive = runRoute {

    pathSingleSlash {getFromResource("web/index.html", `text/html`)} ~
    path("stat") { get { ctx =>
      context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
        case x: Stats =>
          ctx.complete(x.toJson.toString())
      }
    }} ~
    path ("sessions") { get { ctx =>
      ctx.complete("sessions")
    }} ~
    pathPrefix("session") {
      path("start") {
        get {
          parameter('dsn.as[String]) {
            dsn => ctx =>
              ctx.complete("start " + dsn)
          }
        }
      } ~
      path("stop") {
        get {
          complete("stop")
        }
      } ~
      path(Segment) { sid =>
        complete("/" + sid)
      }
    } ~
    path("ping") {
      get {
        parameter('dsn.as[String]) {
          dsn => ctx =>
            val respond = context.actorOf(ResponderActor.props(ctx))
            connectionSupervisorRef.ask(QueryConnection(dsn)).pipeTo(respond)
        }
      }
    }
  }
}
