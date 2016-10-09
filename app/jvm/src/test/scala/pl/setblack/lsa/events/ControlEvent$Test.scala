package pl.setblack.lsa.events

import org.scalatest.{FunSuite, Matchers}

class ControlEvent$Test  extends org.scalatest.FunSpec with Matchers {



  describe("CE") {
    val case1  = ResyncDomain(1, Seq("unpublished"), Map(), true)
    //val case2  = RegisteredClient(1,222)
    it("should write event") {
      val result = ControlEvent.writeEvent(case1)
      result should include ("\"clientId\":\"1\"")
    }



  }
}
