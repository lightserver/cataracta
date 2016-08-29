package pl.setblack.lsa.server

import akka.actor.ActorRef
import pl.setblack.lsa.events.{NodeMessage, Protocol}

class ServerWSProtocol(val subcriber: ActorRef) extends  Protocol{
  override def send(msg: NodeMessage): Unit = {
      subcriber ! msg
  }
}
