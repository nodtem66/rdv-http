import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import org.cardioart.rdv.actor.Session
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._
import akka.pattern.{ask,pipe}
import scala.concurrent.ExecutionContext.Implicits.global

class SessionTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with BeforeAndAfterAll {

  import Session._
  implicit val timeout: Timeout = 5.seconds

  override def afterAll():Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Session" should {
    "return something" in {
      val actor = system.actorOf(Session.props("osdt.n66.info:3333"), "connection-1")
      within(10.seconds) {
        actor ? FlushSession pipeTo testActor
        val result = expectMsgClass(classOf[SessionResult])
        result.map.foreach(t => info(t._1 + ": " + t._2.length))
      }
    }
  }
}
