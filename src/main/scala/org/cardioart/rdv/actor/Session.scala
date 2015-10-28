package org.cardioart.rdv.actor

import akka.actor.Props
import akka.util.Timeout
import com.rbnb.sapi.ChannelMap
import org.cardioart.rdv.actor.Session._

import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

/**
 * Session actor class buffers the stream data with limit buffer length
 * and then flush to API when user request
 * Created by jirawat on 27/10/2015.
 */
object Session {

  case class Parameters(dsn: String, bufferSize: Int, sessionTimeout: Timeout)

  case object FlushSession

  case class SessionResult(map: immutable.Map[String, Array[_]])

  final def props(dsn: String): Props = Props(new Session(Parameters(dsn, 1000, 1000.millis)))
  final def props(params: Parameters): Props = Props(new Session(params))
}

class Session(params: Parameters) extends Connection(params.dsn) {

  import Connection._
  import context.dispatcher

  var bufferMap = new mutable.HashMap[String, ListBuffer[_]]()
  var channelIndexs = Array[Int]()

  override def preStart(): Unit = {
    super.preStart()
    flush()
  }

  override def receive = {
    case StartTimer =>
      timer = context.system.scheduler.schedule(100.millis, 1200.millis, self, RefreshConnection)

    case FlushSession | PingConnection =>

      val m = bufferMap.map((t: (String, ListBuffer[_])) => t._1 -> t._2.toArray).toMap
      bufferMap.foreach(_._2.clear())

      sender ! SessionResult(m)

    case RefreshConnection => flush()

    case _ => log.debug("unknown message")
  }

  protected def flush(): Unit = {

    val cMap = sink.Fetch(params.sessionTimeout.duration.toMillis)

    channelNames = cMap.GetChannelList()
    channelIndexs = channelNames.map(x => cMap.GetIndex(x))

    for ((index, name) <- channelIndexs zip channelNames) {

      cMap.GetType(index) match {
        case ChannelMap.TYPE_INT8 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Byte]()).asInstanceOf[ListBuffer[Byte]]
          buffer ++= cMap.GetDataAsInt8(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) buffer.remove(0, overSize)

        case ChannelMap.TYPE_INT16 | ChannelMap.TYPE_INT32 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Int]()).asInstanceOf[ListBuffer[Int]]
          buffer ++= cMap.GetDataAsInt32(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) buffer.remove(0, overSize)

        case ChannelMap.TYPE_INT64 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Long]()).asInstanceOf[ListBuffer[Long]]
          buffer ++= cMap.GetDataAsInt64(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) buffer.remove(0, overSize)

        case ChannelMap.TYPE_FLOAT32 | ChannelMap.TYPE_FLOAT64 =>
          val buffer = bufferMap.getOrElseUpdate(name, ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
          buffer ++= cMap.GetDataAsFloat64(index)
          val overSize = buffer.length - params.bufferSize
          if (overSize > 0) buffer.remove(0, overSize)

        case _ => ()
      }
    }
  }
}
