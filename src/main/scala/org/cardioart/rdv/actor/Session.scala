package org.cardioart.rdv.actor

import akka.actor.{Cancellable, Actor, ActorLogging, Props}
import akka.util.Timeout
import com.rbnb.sapi.{Sink, ChannelMap}
import org.cardioart.rdv.actor.Session._
import org.cardioart.rdv.parser.{RbnbHost, RbnbSource}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.duration._

/**
 * Session actor class buffers the stream data with limit buffer length
 * and then flush to API when user request
 * Created by jirawat on 27/10/2015.
 */
object Session {

  case class Parameters(dsn: String, bufferSize: Int, sessionTimeout: Timeout)

  case object FlushSession

  case class SessionResult(channel: immutable.Map[String, Array[_]])

  final def props(dsn: String): Props = Props(new Session(Parameters(dsn, 1000, 1000.millis)))
  final def props(params: Parameters): Props = Props(new Session(params))
}

class Session(params: Parameters) extends Actor with ActorLogging {

  import Connection._
  import context.dispatcher

  val RbnbSource(host, port, endpoint, channel) = params.dsn
  val server = RbnbHost(host, port)
  val sink = new Sink()

  var timer: Cancellable = _
  var channelNames = Array[String]()
  var bufferMap = new mutable.HashMap[String, ListBuffer[_]]()
  var channelIndexs = Array[Int]()

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
        flush()
        self ! StartTimer
      }
    } catch {
      case _:Exception =>
        context.parent ! SessionSupervisor.RemoveSession(self.path.toStringWithoutAddress)
        context.stop(self)
    }
  }

  protected def subscribe(): Unit = {
    val cMap = new ChannelMap()
    cMap.Add(f"$endpoint%s/$channel%s/...")
    sink.Subscribe(cMap)
    log.info(f"$endpoint%s/$channel%s/...")
  }
  override def preStart(): Unit = {
    connect(params.dsn)
  }

  override def receive = {
    case StartTimer =>
      timer = context.system.scheduler.schedule(100.millis, 500.millis, self, RefreshConnection)

    case FlushSession =>

      val m = bufferMap.map((t: (String, ListBuffer[_])) => t._1 -> t._2.toArray).toMap
      sender ! SessionResult(m)

      bufferMap.foreach(_._2.clear())

    case RefreshConnection => flush()

    case _ => log.debug("unknown message")
  }

  protected def flush(): Unit = {

    val cMap = sink.Fetch(1000)

    channelNames = cMap.GetChannelList()
    channelIndexs = channelNames.map(x => cMap.GetIndex(x))

    for ((index, name) <- channelIndexs zip channelNames) {

      cMap.GetType(index) match {
        case ChannelMap.TYPE_INT8 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Byte]()).asInstanceOf[ListBuffer[Byte]]
          val timeBuffer = bufferMap.getOrElseUpdate(name + "_time", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          buffer ++= cMap.GetDataAsInt8(index)
          timeBuffer ++= cMap.GetTimes(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) {
            buffer.remove(0, overSize)
            timeBuffer.remove(0, overSize)
          }

        case ChannelMap.TYPE_INT16 | ChannelMap.TYPE_INT32 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Int]()).asInstanceOf[ListBuffer[Int]]
          val timeBuffer = bufferMap.getOrElseUpdate(name + "_time", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          buffer ++= cMap.GetDataAsInt32(index)
          timeBuffer ++= cMap.GetTimes(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) {
            buffer.remove(0, overSize)
            timeBuffer.remove(0, overSize)
          }

        case ChannelMap.TYPE_INT64 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Long]()).asInstanceOf[ListBuffer[Long]]
          val timeBuffer = bufferMap.getOrElseUpdate(name + "_time", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          buffer ++= cMap.GetDataAsInt64(index)
          timeBuffer ++= cMap.GetTimes(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) {
            buffer.remove(0, overSize)
            timeBuffer.remove(0, overSize)
          }

        case ChannelMap.TYPE_FLOAT32 | ChannelMap.TYPE_FLOAT64 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          val timeBuffer = bufferMap.getOrElseUpdate(name + "_time", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          buffer ++= cMap.GetDataAsFloat64(index)
          timeBuffer ++= cMap.GetTimes(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) {
            buffer.remove(0, overSize)
            timeBuffer.remove(0, overSize)
          }

        case _ => ()
      }
    }
  }

  override def postStop() = {
    if (timer ne null) timer.cancel()
    sink.CloseRBNBConnection()
  }
}
