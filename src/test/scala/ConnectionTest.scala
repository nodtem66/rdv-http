import akka.actor.ActorSystem
import akka.pattern.{ask, pipe}
import akka.testkit.TestKit
import akka.util.Timeout
import org.cardioart.rdv.actor.Connection
import org.cardioart.rdv.actor.Connection._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ConnectionTest() extends TestKit(ActorSystem("test-system")) with WordSpecLike with BeforeAndAfterAll {

  implicit val timeout:Timeout = 5.second

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  "A ConnectionActor" should {
    "return something" in {
      val actor = system.actorOf(Connection.props("osdt.n66.info:3333"), "connection-1")
      within(10000.millis) {
        actor ? PingConnection pipeTo testActor
        val channels = expectMsgClass(classOf[Array[String]])
        channels.foreach(ch => info(ch))
      }
    }
  }
}
