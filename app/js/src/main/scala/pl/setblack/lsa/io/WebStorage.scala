package pl.setblack.lsa.io

import org.scalajs.dom
import pl.setblack.lsa.events.MessageListener


class WebStorage extends Storage with MessageListener{
  override def save(value: String, path: Seq[String]): Unit = {
    dom.window.localStorage.setItem(path.toString(), value)
  }

  override def load(path: Seq[String]): Option[String] = {
    dom.window.localStorage.getItem(path.toString()) match {
      case null => None
      case x => Some(x)
    }
  }



}

