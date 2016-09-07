package pl.setblack.lsa.browser

import org.scalajs.dom
import org.scalajs.dom.raw.{Document, WebSocket}
import pl.setblack.lsa.events._
import pl.setblack.lsa.io.{ClientWSProtocol, UriProvider}
import upickle.default._

import scala.concurrent.Promise

class ServerConnection(val  node: Node, val clientIdPromise: Promise[Long]) extends UriProvider{



  val connnectionData  = new ConnectionData()
  var storedToken:Option[String] = None
  var storedId: Option[Long] = None

  val connection: WebSocket = startWs()
  /* def sendBoardMessage( msg: BoardMessage): Unit = {
     connection.send(write(msg))
   }*/

  private def registerServerConnection(clientId: Long, serverId : Long, connection: ServerConnection) = {
    clientIdPromise.success(clientId)
    node.registerConnection(serverId, None, new ClientWSProtocol(connection.connection, connection, node))
  }

  private def processSysMessage(ev: Event, connection: WebSocket): Unit = {

    val ctrlEvent = ControlEvent.parseControlEvent(ev.content)
    ctrlEvent match {
      case RegisteredClient(id, serverId, token) => {
        println(s"registered as ${id}")
        this.storedToken = Some(token)
        this.storedId = Some(id)
        registerServerConnection(id, serverId, this)
        //backend.init(system)
      }
      case x =>
        println(s"have to ignore ${ev.content}")
        //system.mainNode.processSysMessage(ev, connnectionData)
    }
  }

  private def startWs() = {
    val connection = new WebSocket(getWSUri())
    connection.onopen = { (event: org.scalajs.dom.raw.Event) ⇒

    }
    connection.onerror = { (event: org.scalajs.dom.raw.ErrorEvent) ⇒
      println(s"there was an error ${event.toString}")

    }
    connection.onmessage = { (event: org.scalajs.dom.raw.MessageEvent) ⇒
      val msg = read[NodeMessageTransport](event.data.toString).toNodeMessage

      msg.destination.target match {
        case pl.setblack.lsa.events.System => processSysMessage(msg.event, connection)
        case x => println("unknown message")
      }
      // backend.newMessage(msg)
    }
    connection.onclose = { (event: org.scalajs.dom.raw.Event) ⇒
    }

    connection
  }

  private def getWebsocketUri(document: Document, existingId: Option[String], token : Option[String]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    val idString = existingId.getOrElse("begForId")
    val tokenString = token.getOrElse("f7")

    s"$wsProtocol://${dom.document.location.host}/services/board?id=${idString}&token=${tokenString}"
  }


  override def getWSUri(): String = {
    println(s" wsuri generating : ${storedId} ${storedToken}")
    getWebsocketUri(dom.document, storedId.map( _.toString), storedToken)
  }





}
