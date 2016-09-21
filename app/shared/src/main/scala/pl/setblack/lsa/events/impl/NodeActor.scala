package pl.setblack.lsa.events.impl

import pl.setblack.lsa.concurrency.BadActor
import pl.setblack.lsa.events.{Address, Event, Node}
import pl.setblack.lsa.secureDomain.RegisterSigner
import pl.setblack.lsa.security.SigningId
import slogging.{LazyLogging, Logger}

class NodeActor(val node: Node) extends LazyLogging with BadActor[NodeEvent] {
  override def receive(e: NodeEvent): Unit = {
    logger.debug(s"processeing event in NodeActor ${e}")
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
