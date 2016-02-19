package pl.setblack.lsa.events


import pl.setblack.lsa.io.{DomainStorage, Storage}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future, Promise}


/**
  * Node represents system to register domains and send pl.setblack.lsa.events.
  */
class Node(val id: Future[Long])(implicit val storage: Storage) {

  import ExecutionContext.Implicits.global


  private var connections: Map[Long, NodeConnection] = Map()
  private var domains: Map[Seq[String], Domain[_]] = Map()
  private var domainStorages: Map[Seq[String], DomainStorage] = Map()
  private var messageListeners: Seq[MessageListener] = Seq()
  private val loopConnection = registerConnection(id, new LoopInvocation(this))

  private var nextEventId: Long = 0

  def this(constId: Long)(implicit storage: Storage) = {
    this(Promise[Long].success(constId).future)(storage)
  }

  def loadDomains() = {
    this.domains.foreach(
      kv => this.domainStorages.get(kv._1).foreach(
        ds => {
          this.nextEventId += ds.loadEvents(kv._2)

        }
      )
    )
  }

  def registerMessageListener(listener: MessageListener): Unit = {
    messageListeners = messageListeners :+ listener
  }

  def registerDomain[O](path: Seq[String], domain: Domain[O]) = {
    domains = domains + (path -> domain)
    val domainStore = new DomainStorage(path, storage)
    domainStorages = domainStorages + (path -> domainStore)
  }

  private[events] def hasDomain(path: Seq[String]): Boolean = {
    domains contains (path)
  }


  def sendEvent(content: String, domain: Seq[String], transient: Boolean = false): Unit = {
    val adr = Address(All, domain)
    sendEvent(content, adr, transient)
  }

  /**
    * Dispatch event from this Node to ... other Node (or not).
    */
  def sendEvent(content: String, adr: Address, transient: Boolean): Unit = {
    this.id.onSuccess {
      case nodeid: Long => {
        val event = new Event(content, getNextEventId(), nodeid)
        this.sendEvent(event, adr)
      }

    }
  }

  private def sendEvent(event: Event, adr: Address): Unit = {

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

    val result = Promise[Seq[NodeConnection]]
    adr.target match {
      case Local => {

        this.loopConnection onSuccess {
          case nc: NodeConnection => {

            result.success(Seq(nc))

          }
        }
      }
      case All => result.success(this.connections.values.toSeq)
      case System => result.success(this.connections.values.toSeq)
      case Target(x) => result.success(this.connections.values.filter(node => node.knows(x)).toSeq)
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
        this.connections = this.connections + (nodeId -> connection)
        futureConnection.success(connection)
      }
    }

    futureConnection.future
  }

  def getConnections(): Map[Long, NodeConnection] = {
    this.connections
  }

  def registerDomainListener[O](listener: DomainListener[O], path: Seq[String]): Unit = {
    this.filterDomains(path).foreach(x => x match {
      case d: Domain[_] => {
        d.asInstanceOf[Domain[O]].registerListener(listener)
      }
    })
  }


  private def useSerializer[O](syncEvent: ResyncDomain, domain: Domain[O]): Option[DomainSerializer[O]] = {
    if (syncEvent.recentEvents.isEmpty) {
      domain.getSerializer
    } else {
      None
    }
  }

  private def sendRestoreDomain[X](domain: Domain[X], serializer: DomainSerializer[X], address: Address) = {
    val serialized = serializer.write(domain.getState)
    this.id onSuccess {
      case nodeId: Long =>
        val event = Event(write[ControlEvent](RestoreDomain(domain.path, serialized)), 0, nodeId)
        val msg = new NodeMessage(Address(System, domain.path), event, Seq(nodeId))
        this.getConnectionsForAddress(address) onSuccess {
          case seq => seq.foreach(nc => nc.send(msg))
        }
    }
  }

  private def resyncDomain(sync: ResyncDomain, connectionData: ConnectionData): Unit = {
    val address = Address(Target(sync.clientId), sync.domain)

    this.filterDomains(sync.domain).map((domain: Domain[_]) => {
      val castedDomain = domain.asInstanceOf[Domain[Any]]
      useSerializer(sync, castedDomain) match {
        case None => domain.eventsToResend(sync.clientId, sync.recentEvents).foreach(ev => sendEvent(ev, address))
        case Some(serializer) => sendRestoreDomain(castedDomain, serializer, address)
      }

      if (sync.syncBack) {
        this.id onSuccess {
          case nodeId: Long =>
            val event = Event(write[ControlEvent](ResyncDomain(nodeId, sync.domain, domain.recentEvents.mapValues((s: Seq[Long]) => s.max), false)), 0, nodeId)
            val msg = new NodeMessage(Address(System, sync.domain), event, Seq(nodeId))
            this.getConnectionsForAddress(address) onSuccess {
              case seq => seq.foreach(nc => nc.send(msg))
            }
        }
      }
    })
  }

  private def restoreDomain(serialized: RestoreDomain): Unit = {
    this.filterDomains(serialized.domain).foreach(domain => {
      domain.getSerializer match {
        case Some(serializer) => {
          val castedDomain = domain.asInstanceOf[Domain[Any]]
          castedDomain.setState(serializer.read(serialized.serialized))
        }
        case None => ???
      }
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

  private def reroute(msg: NodeMessage): Unit = {
    this.id onSuccess {
      case nodeId: Long => {
        val routedMsg = msg.copy(route = msg.route :+ nodeId)
        this.getConnectionsForAddress(msg.destination).foreach(
         dstCollection =>dstCollection.filter(p => !routedMsg.route.contains(p.targetId)).foreach(nc => {
          nc.send(routedMsg)
        })
        )
      }
    }
  }


  private def filterDomains(path: Seq[String]): Seq[Domain[_]] = {
    val res = this.domains
      .filter((v) => path.startsWith(v._1)).values.toSeq

    res
  }

  /**
    * Node receives message here.
    */
  def receiveMessage(msg: NodeMessage, connectionData: ConnectionData) = {
    receiveMessageLocal(msg, connectionData)
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

  def saveEvent(event: Event, path: Seq[String]) = {
    domainStorages.get(path).foreach(store => store.saveEvent(event))
  }

  private def sendEvenToDomain(event: Event, domain: Domain[_], connectionData: ConnectionData) = {

    val eventContext = new NodeEventContext(this, event.sender, connectionData)
    val result = domain.receiveEvent(event, eventContext)
    if (result.persist) {
      saveEvent(event, domain.path)
    }

  }

  def getDomainObject(path: Seq[String]) = {
    domains.get(path).get.getState
  }

  def getNextEventId(): Long = {
    this.nextEventId += 1
    this.nextEventId
  }

  def resync() = {
    this.domains.foreach(
      kv => syncDomain(kv._1, kv._2, true))
  }


  private def syncDomain(path: Seq[String], domain: Domain[_], syncBack: Boolean) = {
    this.id.onSuccess {
      case nodeId: Long =>
        val event = Event(ControlEvent.writeEvent(ResyncDomain(nodeId, path, domain.recentEvents.mapValues((s: Seq[Long]) => s.max), syncBack)), 0, nodeId)
        val adr = Address(System, path)

        this.sendEvent(event, adr)
    }

  }

  def ping() = {
    val adr = Address(System)
    val pingEvent  = ControlEvent.writeEvent(Ping)
    this.sendEvent(pingEvent, adr, true)
  }
}
