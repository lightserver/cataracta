package pl.setblack.lsa.concurrency

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.japi.Creator


class AkkaConcurrencySystem(val system: ActorSystem) extends  ConcurrencySystem{

  override def createSimpleActor[BAD <: BadActor[E], E](obj: BAD): BadActorRef[E] = {
      val actorRef = system.actorOf(Props(new AkkaActorWrapper[E](obj)))
      new AkkaActorRef[E](actorRef)
  }
}

class AkkaActorRef[E](val akkaActorRef : ActorRef) extends BadActorRef[E] {
  override def send(event: E): Unit = {
      akkaActorRef ! event
  }
}


class AkkaActorCreator[E](val badActor: BadActor[E]) extends Creator[AkkaActorWrapper[E]] {
  @throws[Exception](classOf[Exception])
  override def create(): AkkaActorWrapper[E] = {
    new AkkaActorWrapper[E](badActor)
  }
}

class AkkaActorWrapper[E](val badActor : BadActor[E] ) extends  Actor {
  override def receive = {
    case e:E => badActor.receive(e)
    case _ => println("no nie - znowu pozytywizm...")

  }
}