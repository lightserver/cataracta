package pl.setblack.lsa.secureDomain

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{NodeEvent, SecRegisterSigner}
import pl.setblack.lsa.events.{EventConverter, DefaultResponse, Response, EventContext}

trait SecurityDomainProcessor {
  def nodeRef: BadActorRef[NodeEvent]

 protected def processDomain(state: String, event: SecurityEvent, eventContext: EventContext): Response = {
    event match {
      case register: RegisterSignedCertificate => {
        println("registration of signed cert not implemented yet")
      }
      case signer: RegisterSigner => {
        nodeRef.send(SecRegisterSigner(signer))
      }
    }
    DefaultResponse
  }
}
