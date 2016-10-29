package pl.setblack.lsa.events

import akka.testkit.TestKit
import org.scalatest.{FunSpec, Matchers}
import pl.setblack.lsa.concurrency.{FakeSecurity, NoConcurrencySystem}
import pl.setblack.lsa.os.{Reality, SimpleReality}
import pl.setblack.lsa.resources.FakeResources

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

class NodePersistenceTest extends FunSpec with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global


  describe("node persistence") {

    it(" should save 3 events") {
      val storage = new HashStorage
      val node = new Node(1)(createReality(storage))
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain)
      domainRef.send("nic")
      domainRef.send("nie")
      domainRef.send("moze")
      TestKit.awaitCond({
        val x = storage.storedValues.get(Seq("default")).getOrElse(List())
        println (s"mam ${x}")
        storage.storedValues.get(Seq("default")).getOrElse(List()).size >= 3
      }, 10 seconds)
      storage.storedValues.get(Seq( "default")).map(_.size) should (be(Some(3)))
    }

    it("should load event") {
      val storage = new HashStorage
      val event = UnsignedEvent("nicniema", 1, 1)
      storage.save(Seq("default"), Event.toExportedString(event))
     // storage.save(1.toString, Seq("events", "default", "summary"))
      val node = new Node(1)(createReality(storage))
      val buffer = new ArrayBuffer[String]
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain(buffer))
      domainRef.restoreDomain()
      TestKit.awaitCond(buffer.size >= 1, 10 seconds)
      buffer.mkString should be("nicniema")
    }

    it("should assign correct eventId while loading events ") {
      val storage = presaveEvents(5)
      storage.blockAfter(3)
      val node = new Node(1)(createReality(storage))
      val buffer = new ArrayBuffer[String]
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain(buffer))
      domainRef.restoreDomain()
      domainRef.send("divadlo")
      TestKit.awaitCond(buffer.size >= 2, 10 seconds)
      storage.unlock()
      TestKit.awaitCond(buffer.size >= 6, 10 seconds)
      //send event
      buffer.mkString should endWith("divadlo")
    }

    it("should assign two subsequent correct eventIds while loading events ") {
      val storage = presaveEvents(5)
      storage.blockAfter(3)
      val node = new Node(1)(createReality(storage))
      val buffer = new ArrayBuffer[String]
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain(buffer))
      domainRef.restoreDomain()
      domainRef.send("divadlo")
      domainRef.send("je")
      TestKit.awaitCond(buffer.size >= 2, 10 seconds)
      storage.unlock()
      TestKit.awaitCond(buffer.size >= 7, 10 seconds)
      //send event
      buffer.mkString should include("divadlo")
      buffer.mkString should include("je")
    }
  }

  private def presaveEvents(cnt: Int): HashStorage = {
    val storage = new HashStorage
    var eventNumber: Int = 0
    for (eventNumber <- 1 to cnt) {
      val event = UnsignedEvent(s"Event${eventNumber}", eventNumber, 1)
      storage.save(Seq("default"),Event.toExportedString(event))
    }
    storage
  }


  private def createReality(storage: HashStorage): Reality = {
    val noconcurrency = new NoConcurrencySystem
    val reality: Reality = SimpleReality(storage, noconcurrency, Future {
      new FakeSecurity
    }, new FakeResources)
    reality
  }
}
