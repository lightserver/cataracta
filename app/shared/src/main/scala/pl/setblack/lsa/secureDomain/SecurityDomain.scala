package pl.setblack.lsa.secureDomain

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events._
import pl.setblack.lsa.events.impl.NodeEvent

class SecurityDomain(
                      val x: String,
                      val nodeRef: BadActorRef[NodeEvent]
                    )
  extends Domain[String, SecurityEvent](x, SecurityDomain.securityDomainPath)
    with SecurityDomainProcessor {
}

object SecurityDomain {
  val securityDomainPath = Seq("security")
}