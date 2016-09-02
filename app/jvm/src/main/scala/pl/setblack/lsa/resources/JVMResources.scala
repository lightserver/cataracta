package pl.setblack.lsa.resources
import java.io.FileNotFoundException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JVMResources (implicit val executionContext: ExecutionContext) extends UniResources{
  override def getResource(path: String) : Future[Try[UniResource]] = {
    Future {
      val inputStream  = getClass.getResourceAsStream(s"/$path")
      if ( inputStream != null) {
        Success( new JVMResource(inputStream))
      } else {
        Failure( new FileNotFoundException(s"I do not have $path"))
      }
    }
  }
}
