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

      TestKit.awaitCond(storage.storedValues.filterKeys(key => key.contains("default")).size >= 3, 10 seconds)
      storage.storedValues.get(Seq("events", "default", "summary")).map(_.toInt) should (be(Some(3)))
    }

    it("should load event") {
      val storage = new HashStorage
      val event = UnsignedEvent("nicniema", 1, 1)
      storage.save(Event.toExportedString(event), Seq("events", "default", "1"))
      storage.save(1.toString, Seq("events", "default", "summary"))
      val node = new Node(1)(createReality(storage))
      val buffer = new ArrayBuffer[String]
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain(buffer))
      domainRef.restoreDomain()
      TestKit.awaitCond(buffer.size >= 1, 10 seconds)
      buffer.mkString should be("nicniema")
    }

    it("should assing correct eventIds while loading events ") {
      val storage = presaveEvents(5)
      storage.blockAfter(3)
      val node = new Node(1)(createReality(storage))
      val buffer = new ArrayBuffer[String]
      val domainRef = node.registerDomain(Seq("default"), new TextsDomain(buffer))
      domainRef.restoreDomain()
      domainRef.send("divadlo")
      TestKit.awaitCond(buffer.size >= 2, 10 seconds)
      storage.unlock()
      TestKit.awaitCond(buffer.size >= 5, 10 seconds)
      //send event
      buffer.mkString should endWith("divadlo")
    }
  }

  private def presaveEvents(cnt: Int): HashStorage = {
    val storage = new HashStorage
    var eventNumber: Int = 0
    for (eventNumber <- 1 to cnt) {
      val event = UnsignedEvent(s"Event${eventNumber}", eventNumber, 1)
      storage.save(Event.toExportedString(event), Seq("events", "default", eventNumber.toString))
      storage.save(cnt.toString, Seq("events", "default", "summary"))
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
