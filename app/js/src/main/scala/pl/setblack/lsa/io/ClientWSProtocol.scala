package pl.setblack.lsa.io

import org.scalajs.dom.raw.{MessageEvent, WebSocket}
import pl.setblack.lsa.events._
import upickle.default._

import scala.collection.mutable

class ClientWSProtocol(var connection: WebSocket, val node: Node) extends ProtocolBase {
  setConnectionHandlers(connection)
  override def sendInternal(msg: NodeMessage, connectionData: ConnectionData): Unit = {

    if (connection.readyState > 2) {
      println("restarting connection")
      restartConnection(connection)
      connection.onopen = {  (event: org.scalajs.dom.raw.Event) ⇒
        connection.send(write[NodeMessageTransport](msg.toTransport))
      }
    } else {
      connection.send(write[NodeMessageTransport](msg.toTransport))
    }
  }

  private def setConnectionHandlers( con : WebSocket ):Unit = {

    con.onmessage = { (event : MessageEvent) =>
      val msg = read[NodeMessageTransport](event.data.toString).toNodeMessage
      node.receiveMessage(msg, connectionData)
    }

    con.onerror = { (event: org.scalajs.dom.raw.ErrorEvent) ⇒
      println(s"there was an error ${event.toString}")
      restartConnection(con)
    }
  }

  private def restartConnection( old: WebSocket): Unit = {
    connection = new WebSocket(old.url)
    setConnectionHandlers( connection)
  }
}
