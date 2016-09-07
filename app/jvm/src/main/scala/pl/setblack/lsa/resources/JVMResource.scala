package pl.setblack.lsa.resources

import java.io.InputStream

import scala.io.Source


class JVMResource(val inputStream: InputStream) extends UniResource {
  override def asString: String = {
      val result = Source.fromInputStream(inputStream).mkString
    println(s"result=${result}")
    result
  }
}
