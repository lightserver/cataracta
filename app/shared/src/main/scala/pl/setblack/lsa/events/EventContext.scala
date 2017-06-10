package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{NodeEvent, RegisterDomain}
import pl.setblack.lsa.security.{CertificateInfo, SigningId}

abstract class EventContext {
  def sender: Long

  def reply(eventContent: String, path: Seq[String]): Unit

  def sendLocal(eventContent: String, path: Seq[String]): Unit = {
    val adr = Address(Local, path)
    send(adr, eventContent)
  }

  def send(address: Address, eventContent: String): Unit

  def createDomain[O, EVENT](path : Seq[String], domain : Domain[O, EVENT] ) : Option[DomainRef[EVENT]]

  def getDomainRef[EVENT](addr : Address) (implicit converter: EventConverter[EVENT]) : DomainRef[EVENT]

  def isSecure(): Boolean

  def signedBy: Option[CertificateInfo] = None

  def connectionData: ConnectionData

  def me : Address
}

class NodeEventContext(
                        private val parentNode: Node,
                        override val sender: Long,
                        override val me : Address,
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

  override def createDomain[O, EVENT](path: Seq[String], domain: Domain[O, EVENT]): Option[DomainRef[EVENT]] = {
    Some(parentNode.registerDomain(path, domain))
  }

  override def getDomainRef[EVENT](addr: Address) (implicit converter: EventConverter[EVENT]): DomainRef[EVENT] = {
    new DomainRef[EVENT](addr, parentNode.nodeRef)
  }


}

class NullContext(
                   val nodeRef: BadActorRef[NodeEvent],
                   override val me : Address) extends EventContext {

  def sender = -1

  override val connectionData = new ConnectionData()

  override def reply(eventContent: String, path: Seq[String]): Unit = {

  }

  override def send(address: Address, eventContent: String): Unit = {

  }

  override def isSecure(): Boolean = true

  override def createDomain[O, EVENT](path: Seq[String], domain: Domain[O, EVENT])
    : Option[DomainRef[EVENT]] = {
     nodeRef.send(new RegisterDomain[O,EVENT](path, domain))
    Some(new DomainRef[EVENT](
      path,
      nodeRef
    )(domain.eventConverter))
  }

  override def getDomainRef[EVENT](addr: Address) (implicit converter: EventConverter[EVENT]): DomainRef[EVENT] = {
    new DomainRef[EVENT](addr, nodeRef)
  }
}
