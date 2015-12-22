package pl.setblack.lsa.events

import scala.concurrent._
import scala.concurrent.duration._

class PromiseTest  extends org.scalatest.FunSpec{
  import ExecutionContext.Implicits.global
  describe("onSuccess test") {
    it("should call onSuccess (man)") {
      val promise = Promise[Long].success(7).future
      promise onSuccess {
        case x => println(x)
      }

      promise onSuccess {
        case x => println(x)
      }

      promise onSuccess {
        case x => println(x)
      }

      println("sleeping")
      Thread.sleep(100)
      promise onSuccess {
        case x => println(x)
      }
      }
    }




}
