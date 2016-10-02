package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl._
import pl.setblack.lsa.security.SigningId

class DomainRef[EVENT](
                        val path: Seq[String],
                        val converter: EventConverter[EVENT],
                        val nodeRef: BadActorRef[NodeEvent]

                      ) {

  def send(e: EVENT, endpoint: Endpoint = All): Unit = {
    nodeRef.send(NodeSendEventContent(converter.writeEvent(e), Address(endpoint, path)))
  }

  def sendSigned(e: EVENT, signer: SigningId, endpoint: Endpoint = All): Unit = {
    nodeRef.send(NodeSendSignedEventContent(converter.writeEvent(e), signer, Address(endpoint, path)))
  }

  def restoreDomain() : Unit = {
    nodeRef.send(RestoreDomainNodeEvent(path))
  }

}
