package org.cardioart.rdv.actor

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import org.cardioart.rdv.actor.ConnectionSupervisor._
import org.cardioart.rdv.actor.Session.SessionResult
import org.cardioart.rdv.actor.SessionSupervisor.{InvalidSessionId, SessionList, SessionOperation, SessionTimeout}
import org.cardioart.rdv.parser.MyJsonProtocol._
import spray.http.StatusCodes
import spray.json._
import spray.routing.RequestContext

object ResponderActor {
  final def props(requestContext: RequestContext) = Props(new ResponderActor(requestContext))
}

class ResponderActor(req: RequestContext) extends Actor with ActorLogging {

  def receive = {

    case r: SessionOperation =>
      req.complete(StatusCodes.OK, r.toJson.toString())
      self ! PoisonPill

    case r: SessionList =>
      req.complete(StatusCodes.OK, r.toJson.toString())
      self ! PoisonPill

    case r: SessionResult =>
      req.complete(StatusCodes.OK, r.toJson.toString())
      self ! PoisonPill

    case r: Result =>
      req.complete(StatusCodes.OK, r.toJson.toString())
      self ! PoisonPill

    case SessionTimeout =>
      req.complete(StatusCodes.OK, "{\"error\": \"session timeout\"}")
      self ! PoisonPill

    case InvalidSessionId =>
      req.complete(StatusCodes.OK, "{\"error\": \"invalid session id\"}")
      self ! PoisonPill

    case InvalidDsnError =>
      req.complete(StatusCodes.OK, "{\"error\": \"invalid dsn\"}")
      self ! PoisonPill

    case _ =>
      req.complete(StatusCodes.InternalServerError)
      self ! PoisonPill
  }
}
