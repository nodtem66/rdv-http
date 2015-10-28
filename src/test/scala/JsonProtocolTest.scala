import org.cardioart.rdv.actor.Session.SessionResult
import org.cardioart.rdv.parser.MyJsonProtocol._
import org.scalatest.WordSpec
import spray.json._

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class JsonProtocolTest extends WordSpec {

  "A MyJsonProtocol" should {

    "format JSON of SessionResult" in {
      val result = SessionResult(immutable.Map[String, Array[_]]("a"->Array(1),"b"->Array(2.0)))
      //info(result.toJson.toString)
      assert(result.toJson.toString().length > 2)
    }

    "format JSON of sessionResult from ListBuffer" in {
      var bufferMap = new mutable.HashMap[String, ListBuffer[_]]()
      val buffer = bufferMap.getOrElseUpdate("1", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
      buffer ++= Array(1.0, 2.0, 3.0)

      val m = bufferMap.map((t: (String, ListBuffer[_])) => t._1 -> t._2.toArray).toMap[String, Array[_]]
      val result = SessionResult(m)
      bufferMap.foreach(_._2.clear())

      //info(result.toJson.toString)
      assert(result.toJson.toString().length > 2, "parse JSON error")

    }
  }

  "A HashMap.GetOrElseUpdate" should {
    "update and return if the key is not set" in {
      var bufferMap = new mutable.HashMap[String, ListBuffer[_]]()
      val buffer = bufferMap.getOrElseUpdate("1", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
      buffer ++= Array(1.0, 2.0, 3.0)

      val buffer2 = bufferMap.getOrElseUpdate("1", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
      buffer2 ++= Array(1.0, 2.0, 2.0, 1.0)

      buffer ++= Array(0.0,1,2,3,4,5,6,7,8)
      buffer.remove(0,3)

      val m = bufferMap.map((t: (String, ListBuffer[_])) => t._1 -> t._2.toArray).toMap
      val result = SessionResult(m)
      bufferMap.foreach(_._2.clear())

      val buffer3 = bufferMap.getOrElseUpdate("1", ListBuffer[Double]()).asInstanceOf[ListBuffer[Double]]
      buffer3 ++= Array(100,200,300.0)
      val m2 = bufferMap.map((t: (String, ListBuffer[_])) => t._1 -> t._2.toArray).toMap
      info(SessionResult(m2).toJson.toString)
      info(result.toJson.toString)
    }
  }
}
