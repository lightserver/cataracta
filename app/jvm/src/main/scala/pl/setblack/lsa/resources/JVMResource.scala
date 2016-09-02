package pl.setblack.lsa.resources

import java.io.InputStream

import scala.io.Source


class JVMResource(val inputStream: InputStream) extends UniResource {
  override def asString: String = {
      Source.fromInputStream(inputStream).mkString
  }
}
