package pl.setblack.lsa.events.impl

import pl.setblack.lsa.concurrency.BadActor
import pl.setblack.lsa.events.{Event, Address, Node}

class NodeActor(val node: Node) extends BadActor[NodeEvent] {
  override def receive(e: NodeEvent): Unit = {
      e match {
        case content : NodeSendEventContent =>
          node.sendEvent(content.eventContent, content.adr)
        case event : NodeSendEvent =>
          node.sendEvent(event.event, event.adr)
      }
  }

}




sealed trait NodeEvent

case class NodeSendEventContent(eventContent : String, adr: Address) extends NodeEvent

case class NodeSendEvent(event : Event, adr: Address) extends NodeEvent
