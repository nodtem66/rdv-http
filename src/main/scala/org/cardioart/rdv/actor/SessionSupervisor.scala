package org.cardioart.rdv.actor

import akka.actor._
import akka.pattern.{ask,pipe}
import akka.util.Timeout
import com.rbnb.sapi.SAPIException
import org.cardioart.rdv.actor.Session._
import org.cardioart.rdv.parser.RbnbSource
import org.cardioart.rdv.util.RandomHash

import scala.collection.{immutable, mutable}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object SessionSupervisor {

  case class OpenSession(dsn: String)
  case class QuerySession(sid: String)
  case class CloseSession(sid: String)
  case class ListSession()

  case class SessionOperation(status: String, sid: String)
  case class SessionList(sessions: immutable.Map[String, String])

  case class SessionTimeout()
  case class InvalidSessionId()

  final def props(myClass: Class[_ <: Actor]): Props = Props(new SessionSupervisor(myClass))
  final def props[T <: Actor: ClassTag](): Props = {
    Props(new SessionSupervisor(implicitly[ClassTag[T]].runtimeClass))
  }
}

class SessionSupervisor(childClass: Class[_]) extends Actor with ActorLogging {

  import ConnectionSupervisor._
  import SessionSupervisor._
  import SupervisorStrategy._
  import context.dispatcher

  implicit val timeout: Timeout = 500.millis
  var mapSidDsn = new mutable.HashMap[String, String]()
  var mapSidActor = new mutable.HashMap[String, ActorRef]()


  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _:SAPIException => Restart
    case _:Exception => Restart
  }

  protected def generateSessionId(dsn: String) = RandomHash.generateSHAToken(dsn)

  protected def compileDsn(dsn: String) = dsn match {
    case RbnbSource(host, port, endpoint, channel) =>
      RbnbSource(host, port, endpoint, channel)
    case _ => InvalidDsnError()
  }

  def receive = {

    case OpenSession(dsn) =>
      compileDsn(dsn) match {
        case s:String =>

          val newSid = generateSessionId (s)
          mapSidDsn += newSid -> s

          val actor = context.actorOf (Props (childClass, s), newSid)
          mapSidActor += newSid -> actor

          sender ! SessionOperation ("success", newSid)

        case _ => sender ! InvalidDsnError()
      }

    case CloseSession(sid) =>

      mapSidActor.get(sid) match {
        case Some(ref) =>
          ref ! PoisonPill
          mapSidDsn.remove(sid)
          mapSidActor.remove(sid)
          sender ! SessionOperation ("success", "")
        case _ => sender ! InvalidSessionId()
      }

    case ListSession =>
      sender ! SessionList(mapSidDsn.toMap)

    case QuerySession(sid) =>

      mapSidActor.get(sid) match {
        case Some(ref) =>

          val caller = sender()
          val f = ref.ask(FlushSession).mapTo[SessionResult]

          f onComplete {
            case Success(r) => caller ! r;
            case Failure(_) => caller ! SessionTimeout();
          }

        case _ => sender ! InvalidSessionId()
      }

    case _ => log.debug("unknown message")
  }
}
