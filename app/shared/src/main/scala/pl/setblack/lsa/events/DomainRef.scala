package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl._
import pl.setblack.lsa.security.SigningId

class DomainRef[EVENT]( val path: Seq[String],
                        val nodeRef: BadActorRef[NodeEvent],
                        val endpoint: Endpoint = All
                      ) (implicit val converter: EventConverter[EVENT]) {

  def this( adr: Address,  nodeRef: BadActorRef[NodeEvent])(implicit  converter: EventConverter[EVENT])  = {
    this( adr.path , nodeRef, adr.target) (converter)
  }

  //TODO: i should end with Unit methods
  def send(e: EVENT, endpoint: Endpoint = this.endpoint): Unit = {
    nodeRef.send(NodeSendEventContent(converter.writeEvent(e), Address(endpoint, path)))
  }

  def sendSigned(e: EVENT, signer: SigningId, endpoint: Endpoint = All): Unit = {
    nodeRef.send(NodeSendSignedEventContent(converter.writeEvent(e), signer, Address(endpoint, path)))
  }

  def restoreDomain() : Unit = {
    nodeRef.send(RestoreDomainNodeEvent(path))
  }

}
