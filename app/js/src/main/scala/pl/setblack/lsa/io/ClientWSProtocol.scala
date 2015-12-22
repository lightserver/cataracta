package pl.setblack.lsa.io

import org.scalajs.dom.raw.{MessageEvent, WebSocket}
import pl.setblack.lsa.events.{Node, NodeMessage, NodeMessageTransport, Protocol}
import upickle.default._

class ClientWSProtocol(var connection: WebSocket, val node: Node) extends Protocol {
  override def send(msg: NodeMessage): Unit = {
    if (connection.readyState > 2) {
      println("restarting connection")
      connection = new WebSocket(connection.url)
      connection.onmessage = { (event: MessageEvent) =>
        val msg = read[NodeMessageTransport](event.data.toString).toNodeMessage
        node.receiveMessage(msg)
      }
      connection.onopen = {  (event: org.scalajs.dom.raw.Event) ⇒
        connection.send(write[NodeMessageTransport](msg.toTransport))
      }
    } else {
      connection.send(write[NodeMessageTransport](msg.toTransport))
    }
  }
}
