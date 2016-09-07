package pl.setblack.lsa.resources

import org.scalajs.dom.XMLHttpRequest

case class JSResource(result: XMLHttpRequest) extends UniResource{
  override def asString: String = {
    result.responseText
  }
}
