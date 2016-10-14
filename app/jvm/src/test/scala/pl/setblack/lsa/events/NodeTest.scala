package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.{FakeSecurity, NoConcurrencySystem}
import pl.setblack.lsa.os.{Reality, SimpleReality}
import pl.setblack.lsa.resources.FakeResources

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class NodeTest extends org.scalatest.FunSpec {

  val storage = new FakeStorage
  val noconcurrency = new NoConcurrencySystem
 implicit val reality : Reality = SimpleReality(storage, noconcurrency, Future {
   new FakeSecurity
 }, new FakeResources)
  describe("Node") {


    describe("Domains") {
      val testee = new Node(1)
      it("should not have domain after creation") {
        assert(!testee.hasDomain(Seq("default")))
      }


      it("should register domain") {
        val textDomain = new TextsDomain
        testee.registerDomain(Seq("default"), textDomain)
        assert(testee.hasDomain(Seq("default")))
      }
    }

    describe("listeners") {
      val node1 = new Node(1)
      val history = new HistoryListener
      node1.registerMessageListener(history)
      val testMessage = new NodeMessage(new Address(new Target(1), Seq()), new UnsignedEvent("testik",1,0));
      node1.receiveMessage(testMessage, new ConnectionData())
      assert( history.values(0) == "testik")
    }


    describe("Loop connection") {
      val node1 = new Node(1)
      val node1Addr = new Address(Target(1), Seq(""))
      val nodeAllAddr = new Address(All, Seq(""))
      val nodeLocalAddr = new Address(Local, Seq(""))
      val history = new HistoryListener
      node1.registerMessageListener(history)

      it("should send message to Node1") {
        node1.sendEvent("testLocal", node1Addr)
        wait(node1)
        assert( history.values(0) == "testLocal")
      }
      it("should send message to Node1 via All") {
        node1.sendEvent("testLocal", nodeAllAddr)
        wait(node1)
        assert( history.values(0) == "testLocal")
      }
      it("should send message to Node1 via Local") {
        node1.sendEvent("testLocal", nodeLocalAddr)
        assert( history.values(0) == "testLocal")
      }
    }

    describe("Local connections") {
      val node1 = new Node(1)
      val node2 = new Node(2)
      val connection = new LocalInvocation(node2)
      node1.registerConnection(2, None, connection)
      val node2Addr = new Address(Target(2), Seq(""))
      val history = new HistoryListener
      node2.registerMessageListener(history)

      it("should have registered connection" ) {
        assert( node1.getConnections()(2) != null)
      }

      it("should have registered loop connection" ) {
        assert( node1.getConnections()(1) != null)
      }

      it("should have registered adr target2" ) {
        assert( node1.getConnectionsForAddress(node2Addr) != null)
      }


      it("should send message") {
        node1.sendEvent("test", node2Addr)
        wait(node1)
        assert( history.values(0) == "test")
      }
    }
 }


  def wait(node1: Node): node1.id.type = {
    Thread.sleep(20)
    Await.ready(node1.id, 1 seconds)
  }

  describe("Loop domain  connection") {
    val node1 = new Node(1)
    val nodeLocalAddr = new Address(Local, Seq("default"))

    val domain  = new TextsDomain
    node1.registerDomain(Seq("default"), domain)

    it("should send message to Node1") {
      node1.sendEvent("testLocal", nodeLocalAddr)
      wait(node1)
      assert(domain.getState(0) == "testLocal")
    }
  }
}


class HistoryListener extends MessageListener {
  val values = mutable.Buffer[Any]()
  override def onMessage(m: NodeMessage): Unit = {
    values  +=   m.event.content
  }
}