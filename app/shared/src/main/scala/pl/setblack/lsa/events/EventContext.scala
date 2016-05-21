package pl.setblack.lsa.events

import pl.setblack.lsa.security.{CertificateInfo, SigningId}

abstract class EventContext {
  def sender: Long

  def reply(eventContent: String, path: Seq[String]): Unit

  def sendLocal(eventContent: String, path: Seq[String]): Unit = {
    val adr = Address(Local, path)
    send(adr, eventContent)
  }

  def send(address: Address, eventContent: String): Unit

  def isSecure(): Boolean

  def signedBy: Option[CertificateInfo] = None

  def connectionData: ConnectionData
}

class NodeEventContext(
                        private val parentNode: Node,
                        override val sender: Long,
                        val connectionData: ConnectionData,
                        override val signedBy: Option[CertificateInfo]) extends EventContext {
  override def reply(eventContent: String, path: Seq[String]): Unit = {
    val adr = Address(Target(sender), path)
    parentNode.sendEvent(eventContent, adr)
  }

  override def send(adr: Address, eventContent: String): Unit = {
    parentNode.sendEvent(eventContent, adr)
  }

  override def isSecure(): Boolean = {
    parentNode.id.value.get.get == sender
  }


}

class NullContext extends EventContext {

  def sender = -1

  override val connectionData = new ConnectionData()

  override def reply(eventContent: String, path: Seq[String]): Unit = {

  }

  override def send(address: Address, eventContent: String): Unit = {

  }

  override def isSecure(): Boolean = true


}
