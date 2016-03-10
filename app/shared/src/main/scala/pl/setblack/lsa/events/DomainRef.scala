package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{NodeSendEventContent, NodeEvent}

class DomainRef[EVENT](val path:Seq[String],  val converter: EventConverter[EVENT], val nodeRef: BadActorRef[NodeEvent]) {

   def send(e: EVENT,  endpoint: Endpoint = All ): Unit = {
      nodeRef.send(NodeSendEventContent(converter.writeEvent(e),Address(endpoint, path)) )
   }

}
