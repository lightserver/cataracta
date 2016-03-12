package pl.setblack.lsa.events.impl

import pl.setblack.lsa.concurrency.{BadActorRef, BadActor}
import pl.setblack.lsa.events._
import pl.setblack.lsa.io.DomainStorage
import upickle.default._

class DomainActor[O](val domain: Domain[O], val storage: DomainStorage, val nodeRef: BadActorRef[NodeEvent]) extends BadActor[EventWrapper] {



  override def receive(e: EventWrapper): Unit = {
    e match {
      case LoadDomainCommand => storage.loadEvents(domain) //@todo inc events counter
      case ev: SendEventCommand => {
        val result = domain.receiveEvent(ev.event, ev.ctx)
        if (result.persist) {
          saveEvent(ev.event, domain.path)
        }
      }

      case resync: ResyncDomainCommand => {
        val address = Address(All, resync.sync.domain)
        useSerializer(resync.sync) match {
          case None => domain.eventsToResend(resync.sync.clientId, resync.sync.recentEvents).foreach(ev => sendEvent(ev, address))
          case Some(serializer) => sendRestoreDomain(serializer, address)
        }
        if ( resync.sync.syncBack) {
          syncBack(resync.sync, resync.nodeId, address)
        }
       }
      case restore: RestoreDomainCommand => restoreDomain(restore.sync.serialized)
      case sync: SyncDomainCommand => syncDomain(sync.nodeId, sync.syncBack)
      case regListener  :RegisterListener[ _, _ ] =>
        domain.registerListener( regListener.listener.asInstanceOf[DomainListener[O, domain.EVENT]] )

    }

  }

  private def sendRestoreDomain(serializer: DomainSerializer[O], address: Address) = {
    val serialized = serializer.write(domain.getState)
    sendEvent(write[ControlEvent](RestoreDomain(domain.path, serialized)), Address(System, domain.path))
  }

  private def sendEvent(ev: Event, adr: Address): Unit = {
    nodeRef.send(NodeSendEvent(ev, adr))
  }

  private def sendEvent(eventContent: String, adr: Address): Unit = {
    nodeRef.send(NodeSendEventContent(eventContent, adr))
  }


  private def saveEvent(event: Event, path: Seq[String]) = {
    storage.saveEvent(event)
  }

  private def useSerializer(syncEvent: ResyncDomain): Option[DomainSerializer[O]] = {
    if (syncEvent.recentEvents.isEmpty) {
      domain.getSerializer
    } else {
      None
    }
  }

  def restoreDomain(serialized: String): Unit = {
    this.domain.getSerializer.foreach(serializer => {
      domain.setState(serializer.read(serialized))
    })
  }


  def syncDomain(nodeId: Long, syncBack : Boolean): Unit = {

     val event = Event(ControlEvent.writeEvent(ResyncDomain(nodeId, domain.path, domain.recentEvents.mapValues((s: Seq[Long]) => s.max).toMap, syncBack)), 0, nodeId)
     val adr = Address(System, domain.path)

     this.sendEvent(event, adr)
  }

  def syncBack(sync: ResyncDomain, nodeId: Long, address: Address) = {
   val event = Event(write[ControlEvent](ResyncDomain(nodeId, sync.domain, domain.recentEvents.mapValues((s: Seq[Long]) => s.max).toMap, false)), 0, nodeId)
    this.sendEvent(event, Address(System, address.path))
  }
}


