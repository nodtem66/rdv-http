import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill}
import akka.pattern.{ask, pipe}
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import org.cardioart.rdv.actor.Session._
import org.cardioart.rdv.actor.SessionSupervisor
import org.cardioart.rdv.actor.SessionSupervisor._
import org.scalatest.WordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SessionSupervisorTest extends TestKit(ActorSystem("test-system")) with WordSpecLike {

  implicit val timeout: Timeout = 3000.millis

  "A SessionSupervisor" should {
    "return map of noting when there is no connection" in {
      val supervisor = system.actorOf(SessionSupervisor.props[MockSession]())
      val probe = TestProbe()
      within(2.seconds) {
        supervisor ? ListSession pipeTo probe.ref
        val result = probe.expectMsgClass(classOf[SessionList])
        assert(result.sessions.isEmpty)
        supervisor ! PoisonPill
      }
    }
    "return map when open a connection" in {
      val supervisor = system.actorOf(SessionSupervisor.props[MockSession]())
      val probe = TestProbe()
      within(2.seconds) {
        supervisor ? OpenSession("/1/2/3/4") pipeTo probe.ref
        val result = probe.expectMsgClass(classOf[SessionOperation])
        assert(result.status == "success")
        assert(!result.sid.isEmpty)

        val sid = result.sid
        within(1.seconds) {
          supervisor ? ListSession pipeTo probe.ref
          val result2 = probe.expectMsgClass(classOf[SessionList])
          assert(result2.sessions.nonEmpty)

          within(1.seconds) {
            supervisor ? CloseSession(sid) pipeTo probe.ref
            val result = probe.expectMsgClass(classOf[SessionOperation])
            assert(result.status == "success")
            assert(result.sid == "")
            supervisor ! PoisonPill
          }
        }
      }
    }

    "return a query session" in {
      val supervisor = system.actorOf(SessionSupervisor.props[MockSession]())
      val probe = TestProbe()
      val probe2 = TestProbe()
      val probe3 = TestProbe()

      within(5000.millis) {
        supervisor ? OpenSession("/1/2/3/4") pipeTo probe.ref
        val result = probe.expectMsgClass(classOf[SessionOperation])
        assert(result.status == "success")
        assert(!result.sid.isEmpty)

        val sid = result.sid
        supervisor ? QuerySession("1") pipeTo probe2.ref
        probe2.expectMsg(InvalidSessionId)

        within(2000.millis) {
          supervisor.ask(QuerySession(sid)) pipeTo probe3.ref
          val result = probe3.expectMsgClass(classOf[SessionResult])
          assert(result.map.nonEmpty)
          assert(result.map.isDefinedAt("test"))
          supervisor ! PoisonPill
        }

      }
    }
  }
}

class MockSession(dsn: String) extends Actor with ActorLogging {
  import scala.collection.immutable

  val myMap = immutable.HashMap[String, Array[Int]]("test" -> Array[Int](1,2,3,4))

  //Thread.sleep(2000)

  def receive = {
    case FlushSession =>
      sender ! SessionResult(myMap)
    case _ => ()
  }
}
