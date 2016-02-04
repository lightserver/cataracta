package pl.setblack.lsa.events

import org.scalatest.FunSuite

class ControlEvent$Test  extends org.scalatest.FunSpec {



  describe("CE") {
    val case1  = ResyncDomain(1, Seq("unpublished"), Map(), true)
    //val case2  = RegisteredClient(1,222)
    it("should write event") {
      val resullt = ControlEvent.writeEvent(case1)
      println(resullt)
    }



  }
}
