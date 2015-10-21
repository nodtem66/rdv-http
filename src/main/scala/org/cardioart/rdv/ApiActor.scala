package org.cardioart.rdv

import akka.actor.ActorLogging
import akka.pattern.ask
import akka.util.Timeout
import org.cardioart.rdv.parser.StatsJsonProtocol._
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpServiceActor

import scala.concurrent.duration._

class ApiActor extends HttpServiceActor with ActorLogging {
  implicit val timeout: Timeout = 1.second
  import context.dispatcher

  def receive = runRoute {

    pathSingleSlash {getFromResource("web/index.html", `text/html`)} ~
    path("stat") { get { ctx => {
      context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess { case x: Stats => {
        ctx.complete(x.toJson.toString)
      }}
    }}} ~
    path("ping") {get{complete{"yo!"}}} ~
    path("sessions") {get{complete{"yo!"}}}
  }
}
