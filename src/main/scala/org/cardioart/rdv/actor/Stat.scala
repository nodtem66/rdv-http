package org.cardioart.rdv.actor

import akka.actor.{ActorRef, Actor, ActorLogging}

import scala.collection.mutable.ListBuffer

object Stat {

  case class GetTotalSession(receiver: ActorRef)

  case class GetAliveSession(receiver: ActorRef)

  case class SessionCreated()

  case class SessionDestroy()

  case class Error(from: String, msg: String)

  case class GetLastError(receiver: ActorRef)

  case class GetErrors(receiver: ActorRef)

  val formatErrorMessage: String = "%s error: %s"
}

class Stat extends Actor with ActorLogging {
  var totalSession: Int = 0
  var aliveSession: Int = 0
  var errorListBuffer: ListBuffer[String] = ListBuffer()

  import Stat._

  def receive = {
    case SessionCreated =>
      totalSession += 1
      aliveSession += 1

    case SessionDestroy =>
      if (aliveSession > 0) aliveSession -= 1

    case GetAliveSession(receiver) => receiver ! aliveSession

    case GetTotalSession(receiver) => receiver ! totalSession

    case Error(from, message) => errorListBuffer += formatErrorMessage.format(from, message)

    case GetErrors(receiver) => receiver ! errorListBuffer.toList

    case GetLastError(receiver) => receiver ! errorListBuffer.takeRight(1).toList

    case _ => log.debug("unknow message")
  }
}
