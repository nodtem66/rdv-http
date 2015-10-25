package org.cardioart.rdv.parser

import spray.json._
import org.cardioart.rdv.actor.ConnectionSupervisor._

object ResultJsonProtocol extends DefaultJsonProtocol {
  implicit object ConnectionResultFormat extends RootJsonFormat[Result] {
    def write(r: Result) = JsObject(
      "status" -> JsString(r.status),
      "connections" -> JsArray(r.connections.map(_.toJson).toVector)
    )


    def read(value: JsValue) = value match {
      case _ => deserializationError("Stats can only convert to JSON")
    }
  }

}
