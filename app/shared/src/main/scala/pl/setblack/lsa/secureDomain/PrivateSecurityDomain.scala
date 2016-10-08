package pl.setblack.lsa.secureDomain

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events._
import pl.setblack.lsa.events.impl.{SecRegisterSigner, NodeEvent}

class PrivateSecurityDomain(val x: String, val nodeRef: BadActorRef[NodeEvent] )
  extends PrivateDomain(x )  with SecurityDomainProcessor{

}


object PrivateSecurityDomain {
  val privateSecurityDomainPath = Seq("privateSecurity")
}