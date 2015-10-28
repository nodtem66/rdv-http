import akka.actor.{PoisonPill, Actor, ActorSystem}
import akka.pattern.{ask, pipe}
import akka.testkit.{TestProbe, TestKit}
import akka.util.Timeout
import org.cardioart.rdv.actor.ConnectionSupervisor
import org.cardioart.rdv.actor.ConnectionSupervisor._
import org.scalatest.WordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Success

class ConnectionSupervisorTest() extends TestKit(ActorSystem("test-system")) with WordSpecLike {
  implicit val timeout: Timeout = 1000.millis

  "A ConnectionSupervisor" should {

    "create a new child connection based on dsn" in {
      val supervisor = system.actorOf(ConnectionSupervisor.props[MockConnection](), "connection-supervisor-1")
      within(2000.seconds) {
        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val f1 = supervisor ? QueryConnection(dsn = "/1") pipeTo probe1.ref
        val f2 = supervisor ? QueryConnection(dsn = "/1") pipeTo probe1.ref
        val f3 = supervisor ? QueryConnection(dsn = "/1") pipeTo probe1.ref
        val f123 = Future.sequence(Seq(f1, f2, f3))

        Await.ready(f123, timeout.duration) onComplete {
          case Success(_) =>
            val result = probe1.expectMsgClass(classOf[Result])
            assert(result.status == "online")
          case _ => fail("failed future")
        }

        val f4 = supervisor ? QueryConnection(dsn = "/2") pipeTo probe2.ref
        f4 onComplete {
          case Success(Result(_, cs)) =>
            assert(cs.length == 0)
          case _ => fail("failed future")
        }
        supervisor ! PoisonPill
      }

    }

    "throw error when DSN is invalid" in {
      val supervisor = system.actorOf(ConnectionSupervisor.props[MockConnection](), "connection-supervisor-2")
      within(1200.millis) {
        supervisor ? QueryConnection(dsn = "/") pipeTo testActor
        expectMsg(InvalidDsnError)
        supervisor ! PoisonPill
      }

    }

    "return a status `offline` when timeout PingConnect" in {
      val supervisor = system.actorOf(ConnectionSupervisor.props[SlowConnection](), "connection-supervisor-3")
      within(2000.millis) {
        val probe = TestProbe()
        supervisor ? QueryConnection(dsn = "/1") pipeTo probe.ref
        val result = probe.expectMsgClass(classOf[Result])
        assert(result.status == "offline")
        assert(result.connections.length == 0)
        supervisor ! PoisonPill
      }

    }

    "return a status `offline` when timeout Create" in {
      val supervisor = system.actorOf(ConnectionSupervisor.props[SlowInitConnection](), "connection-supervisor-4")
      within(2000.millis) {
        val probe = TestProbe()
        supervisor ? QueryConnection(dsn = "/1") pipeTo probe.ref
        val result = probe.expectMsgClass(classOf[Result])
        assert(result.status == "offline")
        assert(result.connections.length == 0)
        supervisor ! PoisonPill
      }
    }
  }
}

class MockConnection(dsn: String) extends Actor {
  import org.cardioart.rdv.actor.Connection._
  val numbers: ArrayBuffer[String] = ArrayBuffer()

  def receive = {
    case RefreshConnection => numbers += "0"
    case PingConnection =>
      sender ! numbers.toArray
      numbers += "0"
    case _ => ()
  }
}

class SlowConnection(dsn: String) extends Actor {
  import org.cardioart.rdv.actor.Connection._

  def receive = {
    case PingConnection =>
      Thread.sleep(3000)
      sender ! Array()
    case _ => ()
  }
}

class SlowInitConnection(dsn: String) extends Actor {
  import org.cardioart.rdv.actor.Connection._

  Thread.sleep(2000)

  def receive = {
    case PingConnection =>
      sender ! Array()
    case _ => ()
  }
}