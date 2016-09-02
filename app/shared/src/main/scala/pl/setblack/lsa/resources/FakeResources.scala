package pl.setblack.lsa.resources

import scala.concurrent.Future
import scala.util.{Failure, Try}

class FakeResources extends UniResources {
  import scala.concurrent.ExecutionContext.Implicits.global
  override def getResource(path: String): Future[Try[UniResource]] = {
    Future {
      Failure(new UnsupportedOperationException("sorry thi sis fake only"))
    }
  }
}
