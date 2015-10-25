package org.cardioart.rdv.actor

import akka.actor._
import org.cardioart.rdv.parser.{RbnbSource, RbnbHost}
import scala.concurrent.duration._
import com.rbnb.sapi.{ChannelMap, Sink}

object Connection {
  case class StartTimer()
  case class RefreshConnection()
  case class PingConnection()

  final def props(dsn: String): Props = Props(new Connection(dsn))
}

class Connection(dsn: String) extends Actor with ActorLogging {
  import context.dispatcher
  import Connection._

  val RbnbSource(host, port, endpoint, channel) = dsn
  val server = RbnbHost(host, port)
  val path = endpoint
  val sink = new Sink()
  val channelMap = new ChannelMap()

  var timer: Cancellable = _
  var channelNames = Array[String]()

  /*
   * This is a blocking method to connect to DataTurbine server
   * before doing anything
   */
  private def connect(dsn: String):Unit = {
    sink.CloseRBNBConnection()
    sink.OpenRBNBConnection(server, self.path.name)

    sink.VerifyConnection() match {
      case true =>
        log.info("dsn: {} connected", dsn)
        subscribe()
        fetch()
        self ! StartTimer
      case _ =>
        log.info("dsn: {} reconnect", dsn)

        context.system.scheduler.scheduleOnce(500.millis) { connect(dsn) }
    }
  }

  /**
   * this function used once to subcribe the channels from Dataturbine server
   * see pattern: http://www.dataturbine.org/sites/default/files/programs/RBNB/doc/com/rbnb/sapi/Sink.html
   * #Request(com.rbnb.sapi.ChannelMap, double, double, java.lang.String)
   */
  private def subscribe():Unit = {
    val cMap = new ChannelMap()
    cMap.Add("*/...")
    sink.Subscribe(cMap)
  }

  /**
   * This is a blocking fetch to query channels from DataTurbine server
   */
  private def fetch():Unit = {
    var cMap = new ChannelMap()
    cMap = sink.Fetch(1000)
    channelNames = cMap.GetChannelList()
  }

  override def preStart() = {
    connect(dsn)
  }

  def preRestart():Unit = {
    sink.CloseRBNBConnection()
  }

  def receive = {
    case StartTimer =>
      timer = context.system.scheduler.schedule(100.millis, 1200.millis, self, RefreshConnection)

    case RefreshConnection => fetch()

    case PingConnection => sender ! channelNames

    case msg:String => log.info("unknown message {}", msg)
  }

  def postRestart():Unit = {
    connect(dsn)
  }

  override def postStop() = {

    timer.cancel()
    sink.CloseRBNBConnection()
  }
}
