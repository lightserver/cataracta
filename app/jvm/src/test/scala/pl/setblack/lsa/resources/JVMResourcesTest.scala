package pl.setblack.lsa.resources

import org.scalatest.{AsyncFunSpec, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

class JVMResourcesTest extends AsyncFunSpec with Matchers{

  import ExecutionContext.Implicits.global

  describe("jvmresource") {
    val jvmres = new JVMResources
    it("should read string from file") {
        jvmres.getResource("testresource.txt").map(
          testString =>  testString.get.asString should equal("only a test")
        )
    }
  }


}
