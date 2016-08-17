package pl.setblack.lsa.events.domains

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events._
import pl.setblack.lsa.events.impl._
import pl.setblack.lsa.io.DomainStorage
import pl.setblack.lsa.os.Reality

case class DomainsManager(
                           private val domainRefs: Map[Seq[String], BadActorRef[EventWrapper]] = Map(),
                           private val messageListeners: Seq[MessageListener] = Seq()
                         )(implicit val realityConnection: Reality) {

  type InternalDomainRef = BadActorRef[EventWrapper]

  private[events] def withDomain[O](path: Seq[String], domainRef: BadActorRef[EventWrapper]) = {
    copy(domainRefs = domainRefs + (path -> domainRef))
  }

  private[events] def loadDomains() = {
    this.domainRefs.values.foreach(domainRef => domainRef.send(LoadDomainCommand))
  }

  private[events] def registerMessageListener(listener: MessageListener): DomainsManager = {
    copy(messageListeners = messageListeners :+ listener)
  }

  private[events] def hasDomain(path: Seq[String]): Boolean = {
    domainRefs contains (path)
  }

  private[events] def filterDomains(path: Seq[String]): Seq[BadActorRef[EventWrapper]] = {
    val res = this.domainRefs
      .filter((v) => path.startsWith(v._1)).values.toSeq
    res
  }

  private[events] def receiveLocalDomainMessage(msg: NodeMessage, ctx: EventContext) = {
    messageListeners foreach (listener => listener.onMessage(msg))
    filterDomains(msg.destination.path).foreach((v) => sendEvenToDomain(msg.event, v, ctx))
  }

  private[events] def resync(nodeId : Long) = {
    this.domainRefs.foreach(
      kv => syncDomain(nodeId, kv._1, kv._2, true))
  }

  private def syncDomain(nodeId : Long, path: Seq[String], domain: InternalDomainRef, syncBack: Boolean) = {
        domain.send(new SyncDomainCommand(nodeId, syncBack))
  }

  private def sendEvenToDomain(event: Event, domainRef: InternalDomainRef, eventContext: EventContext) = {
    //val eventContext = new NodeEventContext(this, event.sender, connectionData)
    val wrapped = new SendEventCommand(event, eventContext)
    domainRef.send(wrapped)
  }



}
