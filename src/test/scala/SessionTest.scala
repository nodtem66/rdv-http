import akka.actor.ActorSystem
import akka.pattern.{ask, pipe}
import akka.testkit.TestKit
import akka.util.Timeout
import org.cardioart.rdv.actor.Session
import org.cardioart.rdv.parser.MyJsonProtocol._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SessionTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with BeforeAndAfterAll {

  import Session._
  implicit val timeout: Timeout = 5.seconds

  override def afterAll():Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Session" should {
    "return raw array and JSON format" in {
      val actor = system.actorOf(Session.props(Parameters("osdt.n66.info:3333", 1000, 1.second)), "connection-1")
      within(10.seconds) {
        actor ? FlushSession pipeTo testActor
        val result = expectMsgClass(classOf[SessionResult])
        result.channel.foreach(t => info(t._1 + ": " + t._2.mkString(" ")))
      }
    }
  }
}
