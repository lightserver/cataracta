package pl.setblack.lsa.concurrency

import pl.setblack.lsa.events.impl.NodeSendEventContent
import pl.setblack.lsa.security.SecurityProvider
import slogging.LazyLogging


trait BadActor[-E] {
  def receive(e : E)
}

trait ConcurrencySystem {
  def createSimpleActor[BAD <: BadActor[E],E](obj : BAD) : BadActorRef[E]
}

trait BadActorRef[E] {
  def send( event :  E) : Unit
}


class NoConcurrencySystem extends ConcurrencySystem with LazyLogging{

  override def createSimpleActor[BAD <: BadActor[E], E](obj: BAD): BadActorRef[E] = {
    new JustDoItRef(obj)
  }

  class JustDoItRef[E](val actor : BadActor[E]) extends BadActorRef[E] {
    override def send(event: E): Unit = {
      logger.debug(s"received event  ${event}")
        synchronized(actor.receive(event))
    }
  }
}

class FakeSecurity extends SecurityProvider {

}