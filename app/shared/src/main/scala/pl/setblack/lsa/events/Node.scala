package pl.setblack.lsa.events


import pl.setblack.lsa.concurrency.{BadActorRef, ConcurrencySystem}
import pl.setblack.lsa.events.impl._
import pl.setblack.lsa.io.{DomainStorage, Storage}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future, Promise}


/**
  * Node represents system to register domains and send pl.setblack.lsa.events.
  *
  * @TODO: Increase counter after load
  * @TODO: reorganize counter (target path)
  *
  */
class Node(val id: Future[Long])(implicit val storage: Storage,implicit  val concurency : ConcurrencySystem) {

  import ExecutionContext.Implicits.global

  type InternalDomainRef = BadActorRef[EventWrapper]



  val nodeRef:BadActorRef[NodeEvent] = concurency.createSimpleActor(new NodeActor(this))


  private val connections = new scala.collection.mutable.HashMap[Long, NodeConnection]()
  //private var domains: Map[Seq[String], Domain[_]] = Map()
  private var domainRefs: Map[Seq[String], InternalDomainRef] = Map()

  //private var domainStorages: Map[Seq[String], DomainStorage] = Map()
  private var messageListeners: Seq[MessageListener] = Seq()
  private val loopConnection = registerConnection(id, new LoopInvocation(this))

  private var nextEventId: Long = 0

  def this(constId: Long)(implicit storage: Storage,  concurency : ConcurrencySystem) = {
    this(Promise[Long].success(constId).future)(storage, concurency)
  }

  def loadDomains() = {
    this.domainRefs.values.foreach( domainRef => domainRef.send(LoadDomainCommand))
  }

  def registerMessageListener(listener: MessageListener): Unit = {
    messageListeners = messageListeners :+ listener
  }

  def registerDomain[O](path: Seq[String], domain: Domain[O]) = {
    val actor = new DomainActor(domain, new DomainStorage(path, storage), nodeRef)
    val domainRef:InternalDomainRef = concurency.createSimpleActor( actor)
    domainRefs = domainRefs + ( path -> domainRef)
    new DomainRef[domain.EVENT](path, domain.getEventConverter, nodeRef)
  }

  private[events] def hasDomain(path: Seq[String]): Boolean = {
    domainRefs contains (path)
  }


  def sendEvent(content: String, domain: Seq[String]): Unit = {
    val adr = Address(All, domain)
    sendEvent(content, adr)
  }

  /**
    * Dispatch event from this Node to ... other Node (or not).
    */
  def sendEvent(content: String, adr: Address): Unit = {
    this.id.onSuccess {
      case nodeid: Long => {
        val event = new Event(content, getNextEventId(), nodeid)
        this.sendEvent(event, adr)
      }

    }
  }

  private[events] def sendEvent(event: Event, adr: Address): Unit = {

    this.id onSuccess {
      case nodeId: Long => {

        getConnectionsForAddress(adr) onSuccess {
          case seq => {
            seq.foreach(nc => nc.send(new NodeMessage(adr, event, Seq(nodeId))))
          }
        }
      }
    }

  }

  private[events] def getConnectionsForAddress(adr: Address): Future[Seq[NodeConnection]] = {
    println(s"giving connections for${adr}")
    val result = Promise[Seq[NodeConnection]]
    adr.target match {
      case Local => {

        this.loopConnection onSuccess {
          case nc: NodeConnection => {

            result.success(Seq(nc))

          }
        }
      }
      case All => {
        println(s" ALL will give ${this.connections.size} connections")
        result.success(this.connections.values.toSeq)
      }
      case System => result.success(this.connections.values.toSeq)
      case Target(x) => result.success(this.connections.values.filter(node => node.knows(x)).toSeq)
      case _ => println( s" I got stupid target ${adr.target}")
    }

    result.future
  }

  def createClientIdMessage(clientId: Long): Future[NodeMessage] = {
    this.id.map {
      case nodeId: Long => {

        val event = new Event(write[ControlEvent](RegisteredClient(clientId, nodeId)), 1, nodeId)
        NodeMessage(Address(System), event)
      }
    }
  }


  def registerConnection(id: Long, protocol: Protocol): Future[NodeConnection] = registerConnection(Promise[Long].success(id).future, protocol)


  def registerConnection(futureId: Future[Long], protocol: Protocol): Future[NodeConnection] = {
    val futureConnection = Promise[NodeConnection]
    futureId onSuccess {
      case nodeId: Long => {


        val connection = new NodeConnection(nodeId, protocol)
        this.connections +=  (nodeId -> connection)
        futureConnection.success(connection)
      }
    }

    futureConnection.future
  }

  def getConnections(): Map[Long, NodeConnection] = {
    this.connections.toMap
  }

  def registerDomainListener[O,X](listener: DomainListener[O,X], path: Seq[String]): Unit = {
    this.filterDomains(path).foreach(d =>  d.send( RegisterListener[O,X](listener) ))

  }









  private def resyncDomain(sync: ResyncDomain, connectionData: ConnectionData): Unit = {


    this.filterDomains(sync.domain).map(
      domainRef => {
        domainRef.send( new ResyncDomainCommand(sync))
        /** use serializer and sync back in domain

          * if (sync.syncBack) {
          * this.id onSuccess {
          * case nodeId: Long =>
          * val event = Event(write[ControlEvent](ResyncDomain(nodeId, sync.domain, domain.recentEvents.mapValues((s: Seq[Long]) => s.max), false)), 0, nodeId)
          * val msg = new NodeMessage(Address(System, sync.domain), event, Seq(nodeId))
          * this.getConnectionsForAddress(address) onSuccess {
          * case seq => seq.foreach(nc => nc.send(msg))
          * }
          * }
          * }*/
      }
    )

    /*this.filterDomains(sync.domain).map((domain: Domain[_]) => {
      val castedDomain = domain.asInstanceOf[Domain[Any]]
      useSerializer(sync, castedDomain) match {
        case None => domain.eventsToResend(sync.clientId, sync.recentEvents).foreach(ev => sendEvent(ev, address))
        case Some(serializer) => sendRestoreDomain(castedDomain, serializer, address)
      }*/



  }

  private def restoreDomain(serialized: RestoreDomain): Unit = {
    this.filterDomains(serialized.domain).foreach(domainRef => {
      domainRef.send( new RestoreDomainCommand(serialized))
    })
  }



  def listenDomains(listen: ListenDomains, sender: Long) = {
    this.connections.get(sender).foreach( nodeConnection => nodeConnection.setListeningTo( listen.domains))
  }

  def processSysMessage(ev: Event, connectionData: ConnectionData): Unit = {

    val ctrlEvent = read[ControlEvent](ev.content)
    this.id onSuccess {
      case nodeId: Long =>
        if (ev.sender != nodeId) {
          ctrlEvent match {
            //does not make any sense now...
            case RegisteredClient(clientId, serverId) => println("registered as: " + id)
            case sync: ResyncDomain => resyncDomain(sync, connectionData)
            case serialized: RestoreDomain => restoreDomain(serialized)
            case listen : ListenDomains => listenDomains( listen, ev.sender)
            case Ping => {}
          }
        }
    }

  }

  private def dumoConnections() = {
    println("connections start>>>")
     this.connections.foreach( (mapEntry)=>{ println(s"${mapEntry._1} -> ${mapEntry._2.getClass}")})
    println("<<<connections end")
  }

  private def reroute(msg: NodeMessage): Unit = {
    println(s" wanto to route ${msg.destination} from ${msg.event.sender}")
    dumoConnections()
    this.id onSuccess {
      case nodeId: Long => {
        val routedMsg = msg.copy(route = msg.route :+ nodeId)
        println(s" and what to do with routed ${routedMsg.route}?")
        this.getConnectionsForAddress(msg.destination).foreach(
         dstCollection =>dstCollection.filter(p => !routedMsg.route.contains(p.targetId)).foreach(nc => {
           println(s" will route to: ${nc.targetId}")
          nc.send(routedMsg)
        })
        )
      }
    }
  }


  private def filterDomains(path: Seq[String]): Seq[InternalDomainRef] = {
    val res = this.domainRefs
      .filter((v) => path.startsWith(v._1)).values.toSeq
    res
  }

  /**
    * Node receives message here.
    */
  def receiveMessage(msg: NodeMessage, connectionData: ConnectionData) = {
    receiveMessageLocal(msg, connectionData)
    println(s"after receive local of ${msg.event} for @ ${msg.destination}")
    msg.destination.target match {
      case All => reroute(msg)
      case Target(x) => reroute(msg)
      case _ =>
    }

  }


  def receiveMessageLocal(msg: NodeMessage, connectionData: ConnectionData) = {
    if (msg.destination.target == System) {
      processSysMessage(msg.event, connectionData)
    } else {
      receiveLocalDomainMessage(msg, connectionData)
    }
  }

  private def receiveLocalDomainMessage(msg: NodeMessage, connectionData: ConnectionData) = {
    messageListeners foreach (listener => listener.onMessage(msg))
    filterDomains(msg.destination.path).foreach((v) => sendEvenToDomain(msg.event, v, connectionData))
  }



  private def sendEvenToDomain(event: Event, domainRef: InternalDomainRef, connectionData: ConnectionData) = {

    val eventContext = new NodeEventContext(this, event.sender, connectionData)
    val wrapped = new SendEventCommand(event, eventContext)
    domainRef.send(wrapped)

  }



  def getNextEventId(): Long = {
    this.nextEventId += 1
    this.nextEventId
  }

  def resync() = {
    this.domainRefs.foreach(
      kv => syncDomain(kv._1, kv._2, true))
  }


  private def syncDomain(path: Seq[String], domain: InternalDomainRef, syncBack: Boolean) = {
    this.id.onSuccess {
      case nodeId: Long =>
        domain.send (new SyncDomainCommand(nodeId, syncBack))
    }
  }

  def ping() = {
    val adr = Address(System)
    val pingEvent  = ControlEvent.writeEvent(Ping)
    this.sendEvent(pingEvent, adr)
  }

  case class DomainEvent( event : Event, ctx : EventContext)
}
