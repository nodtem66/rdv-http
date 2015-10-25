import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit._
import org.cardioart.rdv.actor.Stat
import org.cardioart.rdv.actor.Stat._
import org.scalatest.WordSpecLike


class StatTest() extends TestKit(ActorSystem("test-system")) with WordSpecLike {

  "A StatActor" should {
    "return the total session" in {
      val actor = system.actorOf(Props[Stat], "stat-actor1")
      actor ! GetTotalSession(testActor)
      expectMsg(0)
      actor ! SessionCreated
      actor ! SessionCreated
      actor ! SessionCreated
      actor ! GetTotalSession(testActor)
      expectMsg(3)
      actor ! SessionDestroy
      actor ! SessionDestroy
      actor ! GetTotalSession(testActor)
      expectMsg(3)
      actor ! PoisonPill
    }
    "return the alive session" in {
      val actor = system.actorOf(Props[Stat], "stat-actor2")
      actor ! GetAliveSession(testActor)
      expectMsg(0)
      actor ! SessionCreated
      actor ! SessionCreated
      actor ! SessionCreated
      actor ! GetAliveSession(testActor)
      expectMsg(3)
      actor ! SessionDestroy
      actor ! SessionDestroy
      actor ! GetAliveSession(testActor)
      expectMsg(1)
      actor ! PoisonPill
    }
    "return errors list" in {
      val actor = system.actorOf(Props[Stat], "stat-actor3")

      actor ! GetLastError(testActor)
      expectMsg(List())

      actor ! Error(from = "test", msg = "message")
      actor ! GetLastError(testActor)
      expectMsg(List(formatErrorMessage.format("test", "message")))

      actor ! Error(from = "test2", msg = "message2")
      actor ! GetLastError(testActor)
      expectMsg(List(formatErrorMessage.format("test2", "message2")))

      actor ! PoisonPill
    }
    "return last error" in {
      val actor = system.actorOf(Props[Stat], "stat-actor4")
      val result1 = formatErrorMessage.format("test", "message")
      val result2 = formatErrorMessage.format("test2", "message2")

      actor ! GetErrors(testActor)
      expectMsg(List())

      actor ! Error(from = "test", msg = "message")
      actor ! GetErrors(testActor)
      expectMsg(List(result1))

      actor ! Error(from = "test2", msg = "message2")
      actor ! GetErrors(testActor)
      expectMsg(List(result1, result2))

      actor ! PoisonPill
    }
  }
}
