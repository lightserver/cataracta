package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.{FakeSecurity, NoConcurrencySystem}
import pl.setblack.lsa.events.domains.DomainsManager
import pl.setblack.lsa.os.{Reality, SimpleReality}
import pl.setblack.lsa.resources.FakeResources
import pl.setblack.lsa.server.JVMRealityConnection

import scala.concurrent.Future

class DomainsManagerTest extends org.scalatest.FunSpec{
  import scala.concurrent.ExecutionContext.Implicits.global

  val storage = new FakeStorage
  val noconcurrency = new NoConcurrencySystem
  implicit val reality : Reality = SimpleReality(storage, noconcurrency, Future {
    new FakeSecurity
  }, new FakeResources)

  describe("Domains") {
    val testee = new DomainsManager()
    it("should not have domain after creation") {
      assert(!testee.hasDomain(Seq("default")))
    }

  }
}
