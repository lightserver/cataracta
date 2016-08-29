package pl.setblack.lsa.server

import akka.actor.{ActorRef, ActorSystem}
import pl.setblack.lsa.events.{NodeMessage, NodeMessageTransport, Protocol}
import upickle.default._

/**
  Akka  To Akka Protocol
 */
class ServerRemoteProtocol(val remote : ActorRef, val senderId: String)( implicit  system: ActorSystem )  extends Protocol {

  override def send(msg: NodeMessage): Unit = {
      remote ! ReceivedMessage(senderId,  write[NodeMessageTransport](msg.toTransport))
  }
}
