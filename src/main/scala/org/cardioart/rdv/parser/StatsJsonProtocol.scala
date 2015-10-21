package org.cardioart.rdv.parser

import spray.can.server.Stats
import spray.json._

object StatsJsonProtocol extends DefaultJsonProtocol {
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
}
