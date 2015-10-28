package org.cardioart.rdv.parser

import org.cardioart.rdv.actor.ConnectionSupervisor._
import org.cardioart.rdv.actor.Session._
import org.cardioart.rdv.actor.SessionSupervisor._
import spray.can.server.Stats
import spray.json._
import spray.json.JsField

import scala.collection.mutable.ListBuffer

object MyJsonProtocol extends DefaultJsonProtocol {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case d: Double => JsNumber(d)
      case s: String => JsString(s)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
    }
    def read(value: JsValue) = (value: @unchecked) match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
    }
  }
  implicit object StatJsonFormat extends RootJsonFormat[Stats] {
    def write(s: Stats) = JsObject(
      "uptime" -> JsNumber(s.uptime.toSeconds),
      "total_request" -> JsNumber(s.totalRequests),
      "open_request" -> JsNumber(s.openRequests),
      "total_connection" -> JsNumber(s.totalConnections),
      "open_connection" -> JsNumber(s.openConnections),
      "request_timeout" -> JsNumber(s.requestTimeouts)
    )

    def read(value: JsValue) = value match {
      case _ => deserializationError("Stats can only convert to JSON")
    }
  }

  implicit object ConnectionResultFormat extends RootJsonFormat[Result] {
    def write(r: Result) = JsObject(
      "status" -> JsString(r.status),
      "connections" -> JsArray(r.connections.map(_.toJson).toVector)
    )
    def read(value: JsValue) = value match {
      case _ => deserializationError("not support JSON to Result")
    }
  }

  implicit object SessionResultFormat extends RootJsonFormat[SessionResult] {
    def write(r: SessionResult) = {
      var field = ListBuffer[JsField]()

      for ((k,v) <- r.map) {
        if (v.isInstanceOf[Array[Double]])
          field += (k -> v.asInstanceOf[Array[Double]].toJson)
        else if (v.isInstanceOf[Array[Int]])
          field += (k -> v.asInstanceOf[Array[Int]].toJson)
        else if (v.isInstanceOf[Array[String]])
          field += (k -> v.asInstanceOf[Array[String]].toJson)
        else if (v.isInstanceOf[Array[Byte]])
          field += (k -> v.asInstanceOf[Array[Byte]].toJson)
      }
      JsObject(field.toList: _*)
    }
    def read(value: JsValue) = value match {
      case _ => deserializationError("not support JSON to SessionResult")
    }
  }

  implicit object SessionListFormat extends RootJsonFormat[SessionList] {
    def write(r: SessionList) = JsArray(r.sessions.map(_.toJson).toVector)
    def read(value: JsValue) = value match {
      case _ => deserializationError("not support JSON to SessionList")
    }
  }

  implicit object SessionOperationFormat extends RootJsonFormat[SessionOperation] {
    def write(r: SessionOperation) = JsObject(
      "status" -> JsString(r.status),
      "return" -> JsString(r.sid)
    )
    def read(value: JsValue) = value match {
      case _ => deserializationError("not support JSON to SessionOperation")
    }
  }

}