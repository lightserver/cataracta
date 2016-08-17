package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.{FakeSecurity, NoConcurrencySystem}
import pl.setblack.lsa.events.domains.DomainsManager
import pl.setblack.lsa.os.{Reality, SimpleReality}

import scala.concurrent.Future

class DomainsManagerTest extends org.scalatest.FunSpec{
  import scala.concurrent.ExecutionContext.Implicits.global

  val storage = new FakeStorage
  val noconcurrency = new NoConcurrencySystem
  implicit val reality : Reality = SimpleReality(storage, noconcurrency, Future {
    new FakeSecurity
  })

  describe("Domains") {
    val testee = new DomainsManager()
    it("should not have domain after creation") {
      assert(!testee.hasDomain(Seq("default")))
    }

  }
}
