package pl.setblack.lsa.events

import pl.setblack.lsa.io.Storage

class FakeStorage extends Storage{
  override def save(value: String, path: Seq[String]): Unit = {

  }

  override def load(path: Seq[String]): Option[String] =  None

}
