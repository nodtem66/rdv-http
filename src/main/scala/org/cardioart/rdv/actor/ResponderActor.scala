package org.cardioart.rdv.actor

import akka.actor.{ActorLogging, PoisonPill, Actor, Props}
import org.cardioart.rdv.actor.ConnectionSupervisor._
import org.cardioart.rdv.parser.ResultJsonProtocol._
import spray.http.StatusCodes
import spray.json._
import spray.routing.RequestContext

object ResponderActor {
  final def props(requestContext: RequestContext) = Props(new ResponderActor(requestContext))
}

class ResponderActor(req: RequestContext) extends Actor with ActorLogging {

  def receive = {
    case r: Result =>
      req.complete(StatusCodes.OK, r.toJson.toString())
      self ! PoisonPill
    case InvalidDsnError =>
      req.complete(StatusCodes.OK, "invalid dsn")
      self ! PoisonPill
    case _ =>
      req.complete(StatusCodes.InternalServerError)
      self ! PoisonPill
  }
}
