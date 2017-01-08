package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{NodeEvent, RegisterDomain, RegisterDomainListener}

/**
  * Created by jarek on 1/8/17.
  */
class NodeRef(private val nodeRef : BadActorRef[NodeEvent]) {

  def registerDomain[O, EVENT](path :Seq[String], domain: Domain[O, EVENT]):DomainRef[EVENT] = {
    nodeRef.send(RegisterDomain(path, domain))
    new DomainRef[EVENT](
      path,
      nodeRef
    )(domain.eventConverter)
  }

  def registerDomainListener[O, EVENT](path: Seq[String], listener: DomainListener[O, EVENT] ) = {
    nodeRef.send(RegisterDomainListener(path, listener))
  }

}
