package pl.setblack.lsa.concurrency

import pl.setblack.lsa.security.SecurityProvider
import slogging.LazyLogging


trait BadActorRef[E] {
  def send( event :  E) : Unit
}


trait BadActor[E] {
  def receive(e : E, self : BadActorRef[E] )

}


trait ConcurrencySystem {
  def createSimpleActor[BAD <: BadActor[E],E](obj : BAD) : BadActorRef[E]
}



class NoConcurrencySystem extends ConcurrencySystem with LazyLogging{

  override def createSimpleActor[BAD <: BadActor[E], E](obj: BAD): BadActorRef[E] = {
    new JustDoItRef(obj)
  }

  class JustDoItRef[E](val actor : BadActor[E]) extends BadActorRef[E] {
    override def send(event: E): Unit = {
      logger.debug(s"received event  ${event}")
        synchronized(actor.receive(event, this))
    }
  }
}

class FakeSecurity extends SecurityProvider {

}