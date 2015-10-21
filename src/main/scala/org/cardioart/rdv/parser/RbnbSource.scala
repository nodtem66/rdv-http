package org.cardioart.rdv.parser

/**
 * This object represents the Rbnb data source name.
 * e.g.
 * localhost:3333/device_1/measurement_1
 * localhost/device_1/m1
 * /device_1/m1
 * device_1/m1
 */
object RbnbSource {
  // injection method
  def apply(host: String, port: String, endpoint: String, channel: String) =
    host + ":" + port + "/" + endpoint + "/" + channel

  // extraction method
  def unapply(str: String): Option[(String, String, String, String)] = {

    val segments: Array[String] = str split "/"

    segments match {
      // case `endpoint/` with split into Array("endpoint")
      case Array(single) =>
        if (str.endsWith("/")) Some("","", single, "")
        else None
      // case `_/__`
      case Array(head, tail) =>
        head match {
          case RbnbHost(host, port) => Some(host,port, tail, "")
          case h:String if (h.isEmpty || h.contains(":")) => Some("", "", tail, "")
          case _ => Some("", "", head, tail)
        }

      case Array(address, endpoint, channel, _*) =>
        address match {
          case RbnbHost(host, port) => Some(host, port, endpoint, channel)
          case h:String if h.isEmpty => Some("", "", endpoint, channel)
          case _ => None
        }
      case _ => None
    }
  }
}

object RbnbHost {
  def apply(host:String, port:String) = host + ":" + port
  def unapply(str: String): Option[(String, String)] = {
    val seq = str split ":"
    if (seq.length == 2) if (seq(0).isEmpty) None else Some(seq(0), seq(1))
    else None
  }
}