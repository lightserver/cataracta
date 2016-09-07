package pl.setblack.lsa.resources
import java.io.{FileNotFoundException, IOException}

import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JSResources(implicit val executionContext: ExecutionContext) extends UniResources{
  override def getResource(path: String): Future[Try[UniResource]] = {
    val resultPromise:Future[XMLHttpRequest] = Ajax.get(s"/resources/${path}")
    resultPromise.map( result =>
    if ( result.status == 200) {
        Success(new JSResource(result))
    } else {
      Failure(new IOException(s"response ${result.status} reading ${path}") )
    })

  }
}
