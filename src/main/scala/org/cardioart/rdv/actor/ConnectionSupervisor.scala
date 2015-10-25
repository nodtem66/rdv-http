package org.cardioart.rdv.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import org.cardioart.rdv.actor.Connection._
import org.cardioart.rdv.parser.RbnbSource

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object ConnectionSupervisor {
  case class QueryConnection(dsn: String)
  case class Result(status: String, connections: Array[String])
  case class InvalidDsnError()

  final def props(myClass: Class[_ <: Actor]): Props = Props(new ConnectionSupervisor(myClass))
  final def props[T <: Actor: ClassTag](): Props = {
    Props(new ConnectionSupervisor(implicitly[ClassTag[T]].runtimeClass))
  }
}

class ConnectionSupervisor(actorClass: Class[_]) extends Actor with ActorLogging {
  import ConnectionSupervisor._
  implicit val timeout: Timeout = 500.millis
  val connection  = mutable.HashMap[String, ActorRef]()
  var index = 0

  def receive = {
    case QueryConnection(dsn) =>
      // compile DSN
      dsn match {
        case RbnbSource(host, port, endpoint, channel) =>
          // valid DSN
          val dsnName = RbnbSource(host, port, endpoint, channel)

          connection.get(dsnName) match {
            case Some(ref) =>
              // query connection from created child actor
              query(ref, sender)

            case _ =>
              // if child's actor've not created; create it
              val actor = context.actorOf(Props(actorClass, dsnName), f"connection-$index%d")
              // save to hashmap
              connection += dsnName -> actor
              index += 1

              query(actor, sender)
          }
        case s:String => sender ! InvalidDsnError
      }
    case msg:String => log.info("unknown message {}", msg)
  }

  private def query(child: ActorRef, caller: ActorRef): Unit = {

    val f = child.ask(PingConnection).mapTo[Array[String]]

    f.onComplete {
      case Success(c) =>
        caller ! Result("online", c)
      case Failure(_) =>
        caller ! Result("offline", Array())
    }
  }
}
