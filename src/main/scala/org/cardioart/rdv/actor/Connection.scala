package org.cardioart.rdv.actor

import akka.actor._
import com.rbnb.sapi.{ChannelMap, Sink}
import org.cardioart.rdv.parser.{RbnbHost, RbnbSource}

import scala.concurrent.duration._

object Connection {
  case class StartTimer()
  case class RefreshConnection()
  case class PingConnection()

  final def props(dsn: String): Props = Props(new Connection(dsn))
}

class Connection(dsn: String) extends Actor with ActorLogging {
  import Connection._
  import context.dispatcher

  val RbnbSource(host, port, endpoint, channel) = dsn
  val server = RbnbHost(host, port)
  val sink = new Sink()

  var timer: Cancellable = _
  var channelNames = Array[String]()

  /*
   * This is a blocking method to connect to DataTurbine server
   * before doing anything
   */
  protected def connect(dsn: String):Unit = {
    try {
      sink.CloseRBNBConnection()
      sink.OpenRBNBConnection(server, self.path.name)

      if (sink.VerifyConnection()) {
        log.debug("dsn: {} connected", dsn)

        subscribe()
        fetch()
        self ! StartTimer
      }
    } catch {
      case _:Exception =>
        context.parent ! ConnectionSupervisor.CloseConnection(dsn)
        context.stop(self)
    }
  }

  /**
   * this function used once to subcribe the channels from Dataturbine server
   * see pattern: http://www.dataturbine.org/sites/default/files/programs/RBNB/doc/com/rbnb/sapi/Sink.html
   * #Request(com.rbnb.sapi.ChannelMap, double, double, java.lang.String)
   */
  protected def subscribe():Unit = {
    val cMap = new ChannelMap()
    cMap.Add("*/...")
    sink.Subscribe(cMap)
  }

  /**
   * This is a blocking fetch to query channels from DataTurbine server
   */
  protected def fetch():Unit = {
    val cMap = sink.Fetch(1000)
    channelNames = cMap.GetChannelList()
  }

  override def preStart() = {
    connect(dsn)
  }

  def receive = {
    case StartTimer =>
      timer = context.system.scheduler.schedule(100.millis, 1200.millis, self, RefreshConnection)

    case RefreshConnection => fetch()

    case PingConnection => sender ! channelNames

    case _ => log.debug("unknown message")
  }

  override def postStop() = {
    if (timer ne null) timer.cancel()
    sink.CloseRBNBConnection()
  }
}
