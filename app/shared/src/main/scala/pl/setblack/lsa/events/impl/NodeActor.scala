package pl.setblack.lsa.events.impl

import pl.setblack.lsa.concurrency.{BadActor, BadActorRef}
import pl.setblack.lsa.events._
import pl.setblack.lsa.secureDomain.RegisterSigner
import pl.setblack.lsa.security.SigningId
import slogging.{LazyLogging, Logger}

class NodeActor(val node: Node) extends LazyLogging with BadActor[NodeEvent] {
  override def receive(e: NodeEvent, self : BadActorRef[NodeEvent]): Unit = {

    e match {
      case content: NodeSendEventContent =>
        node.sendEvent(content.eventContent, content.adr)

      case event: NodeSendEvent =>
        node.sendEvent(event.event, event.adr)
      case signed: NodeSendSignedEventContent =>
        node.sendSignedEvent(signed.eventContent, signed.adr, signed.signer)
      case regSigner: SecRegisterSigner =>
        node.registerSigner(regSigner)
      case gen: SecGenerateKeyPair =>
        node.generateKeyPair(gen.author, gen.certifiedBy, gen.privileges, gen.adr)
      case restore : RestoreDomainNodeEvent =>
        node.restoreDomain(restore.path)
      case deblock : FoundMaxEventForDomain => node.deblock(deblock.maxEvenID)

      case register : RegisterDomain[_,_] => node.registerDomain(register.path, register.domain)

      case listener : RegisterDomainListener[_,_] => node.registerDomainListener(listener.listener, listener.path)
    }
  }
}

sealed trait NodeEvent

case class NodeSendEventContent(eventContent: String, adr: Address) extends NodeEvent

case class NodeSendSignedEventContent(eventContent: String, signer: SigningId, adr: Address) extends NodeEvent

case class NodeSendEvent(event: Event, adr: Address) extends NodeEvent

case class SecRegisterSigner(secEvent: RegisterSigner) extends NodeEvent

case class SecGenerateKeyPair(
                               author: SigningId,
                               certifiedBy: SigningId,
                               privileges: Set[String],
                               adr: Address) extends NodeEvent

case class RegisterDomain[O, EVENT](path :Seq[String], domain: Domain[O, EVENT]) extends NodeEvent

case class RegisterDomainListener[O, EVENT](path :Seq[String], listener: DomainListener[O, EVENT]) extends NodeEvent

case class RestoreDomainNodeEvent( path: Seq[String]) extends NodeEvent

case class FoundMaxEventForDomain(maxEvenID : Long, path: Seq[String]) extends NodeEvent
