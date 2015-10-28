package org.cardioart.rdv.actor

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.cardioart.rdv.actor.ConnectionSupervisor._
import org.cardioart.rdv.actor.Session.Parameters
import org.cardioart.rdv.actor.SessionSupervisor._
import org.cardioart.rdv.parser.MyJsonProtocol.StatJsonFormat
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpServiceActor


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
      val respond = context.actorOf(ResponderActor.props(ctx))
      sessionSupervisorRef.ask(ListSession).pipeTo(respond)
    }} ~
    pathPrefix("session") {
      path("start") {
        get {
          parameters('dsn.as[String], 'limit.as[Int] ? 1000, 't.as[Int] ? 1000) {
            (dsn,limit,t) => ctx =>
              val respond = context.actorOf(ResponderActor.props(ctx))
              sessionSupervisorRef.ask(OpenSession(Parameters(dsn, limit, t.millis))).pipeTo(respond)
          }
        }
      } ~
      path("stop") {
        get {
          parameter('sid.as[String]) {
            sid => ctx =>
              val respond = context.actorOf(ResponderActor.props(ctx))
              sessionSupervisorRef.ask(CloseSession(sid)).pipeTo(respond)
          }
        }
      } ~
      path(Segment) { sid => ctx =>
        val respond = context.actorOf(ResponderActor.props(ctx))
        sessionSupervisorRef.ask(QuerySession(sid)).pipeTo(respond)
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
