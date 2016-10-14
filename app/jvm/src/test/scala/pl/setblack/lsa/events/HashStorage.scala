package pl.setblack.lsa.events

import java.util.UUID

import pl.setblack.lsa.io.Storage

class HashStorage extends Storage {
  var storedValues = Map[Seq[String], String]()
  val uui = UUID.randomUUID()
  override def save(value: String, path: Seq[String]): Unit = {
    println (s"storing ${path} -> ${value} in ${uui}")
    storedValues = storedValues + (path -> value)
  }

  override def load(path: Seq[String]): Option[String] =
    storedValues.get(path)
}
