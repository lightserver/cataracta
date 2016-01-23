package pl.setblack.lsa.io

import org.scalajs.dom.raw.{MessageEvent, WebSocket}
import pl.setblack.lsa.events._
import upickle.default._

import scala.collection.mutable

class ClientWSProtocol(var connection: WebSocket, val node: Node) extends Protocol {
  val connectionData = mutable.Map[Long, ConnectionData]()
  setConnectionHandlers(connection)

  override def send(msg: NodeMessage): Unit = {
    if (connection.readyState > 2) {
      println("restarting connection")
      restartConnection(connection)
      connection.onopen = {  (event: org.scalajs.dom.raw.Event) ⇒
        println("will send wanted data")
        connection.send(write[NodeMessageTransport](msg.toTransport))
      }
    } else {
      connection.send(write[NodeMessageTransport](msg.toTransport))
    }
  }

  private def setConnectionHandlers( con : WebSocket ):Unit = {

    con.onmessage = { (event : MessageEvent) =>
      println(s"received to ${event.data.toString}")
      val msg = read[NodeMessageTransport](event.data.toString).toNodeMessage
      println("converted")
      node.receiveMessage(msg, connectionData.getOrElseUpdate(msg.event.sender, new ConnectionData()))

    }


    con.onerror = { (event: org.scalajs.dom.raw.ErrorEvent) ⇒
      println(s"there was an error ${event.toString}")
      restartConnection(con)
    }
  }

  private def restartConnection( old: WebSocket): Unit = {
    println(s"restarting connection ${old.url}")
    connection = new WebSocket(old.url)
    setConnectionHandlers( connection)
  }
}
