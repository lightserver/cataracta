package pl.setblack.lsa.io

import org.scalajs.dom
import pl.setblack.lsa.events.MessageListener

import scala.scalajs.js._
import upickle.default._

class WebStorage extends Storage with MessageListener{
  override def save(value: String, path: Seq[String]): Unit = {

    dom.localStorage.setItem(path.toString(), value)

  }

  override def load(path: Seq[String]): Option[String] = {
    dom.localStorage.getItem(path.toString()) match {
      case null => None
      case x => Some(x)
    }
  }



}

