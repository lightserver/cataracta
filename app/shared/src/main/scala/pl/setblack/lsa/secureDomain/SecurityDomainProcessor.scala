package pl.setblack.lsa.secureDomain

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{NodeEvent, SecRegisterSigner}
import pl.setblack.lsa.events.{EventConverter, DefaultResponse, Response, EventContext}

trait SecurityDomainProcessor {
  def nodeRef: BadActorRef[NodeEvent]
   type EVENT = SecurityEvent



  def getEventConverter: EventConverter[EVENT] =  SecurityEventConverter

   protected def processDomain(state: String, event: SecurityEvent, eventContext: EventContext): Response = {
     println(s"processing security domain ${event}")
    event match {
      case register: RegisterSignedCertificate => {

      }
      case signer: RegisterSigner => {
        nodeRef.send(SecRegisterSigner(signer))
      }
    }
    DefaultResponse
  }
}
