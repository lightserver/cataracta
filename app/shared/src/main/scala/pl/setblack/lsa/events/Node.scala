package pl.setblack.lsa.events



import pl.setblack.lsa.io.{DomainStorage, Storage}
import upickle.default._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise, ExecutionContext, Future}


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

  def this(constId:Long ) (implicit  storage: Storage)= {
    this( Promise[Long].success(constId).future)(storage)
  }
  def loadDomains() = {
    this.domains.foreach(
      kv => this.domainStorages.get(kv._1).foreach(
        ds => {
          this.nextEventId += ds.loadEvents(kv._2)
          println(s"-----next event id is:" + this.nextEventId)
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
    println(s"have domains:${domains}")
  }

  private[events] def hasDomain(path: Seq[String]): Boolean = {
    domains contains (path)
  }


  def sendEvent(content: String, domain: Seq[String], transient : Boolean = false): Unit = {
    val adr = Address(All, domain)
    println("sending event to:" + adr.toString)
    sendEvent(content, adr, transient)
  }

  /**
   * Dispatch event from this Node to ... other Node (or not).
   */
  def sendEvent(content: String, adr: Address, transient : Boolean): Unit = {
    this.id.onSuccess {
        case nodeid:Long => {
          val event = new Event(content, getNextEventId(), nodeid, transient)
          this.sendEvent(event, adr)
        }

    }
  }

  private def sendEvent(event: Event, adr: Address): Unit = {
    println(s"will  send event ${event} to ${adr}")
    this.id onSuccess {
      case nodeId:Long => {
        println(s" sending event ${event} to ${adr}")
        getConnectionsForAddress(adr) onSuccess {
          case seq => {
             println("got connection...")
            seq.foreach(nc => nc.send(new NodeMessage(adr, event, Seq(nodeId))))
          }
        }
      }
    }

  }

  private[events] def getConnectionsForAddress(adr: Address): Future[Seq[NodeConnection]] = {
    println(s"looking for  connections ${adr}")
    val result = Promise[Seq[NodeConnection]]
      adr.target match {
      case Local => {
        println("referencing local...")
        this.loopConnection onSuccess {
          case nc: NodeConnection => {
            println("got local NC")
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
      case nodeId:Long => {
        println(s" NodeMessage for clientId ${clientId}")
      val event = new Event(write[ControlEvent](RegisteredClient(clientId,nodeId)), 1, nodeId)
      NodeMessage(Address(System), event)
    }
    }
  }


  def registerConnection(id: Long, protocol: Protocol): Future[NodeConnection] = registerConnection(Promise[Long].success(id).future, protocol)


  def registerConnection(futureId: Future[Long], protocol: Protocol): Future[NodeConnection] = {
    val futureConnection = Promise[NodeConnection]
    futureId onSuccess {
      case nodeId: Long => {
        println(s"added future connection ${nodeId}")

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
        println("filtered domain:" + path)
        d.asInstanceOf[Domain[O]].registerListener(listener)
      }
    })
  }

  private def resyncDomain(sync: ResyncDomain): Unit = {
    val address = Address(Target(sync.clientId), sync.domain)

    this.filterDomains(sync.domain).map(domain => {
      domain.resendEvents(sync.clientId, sync.recentEvents).foreach(ev => sendEvent(ev, address))
      if (sync.syncBack) {
        this.id onSuccess {
          case nodeId:Long =>
            val event = Event(write[ControlEvent](ResyncDomain(nodeId, sync.domain,  domain.recentEvents, false)), 0, nodeId)
            val msg = new NodeMessage(Address(System, sync.domain), event, Seq(nodeId))
            this.getConnectionsForAddress(address) onSuccess {
              case seq => seq.foreach(nc => nc.send(msg))
            }
        }

      }
    })
  }

  def processSysMessage(ev: Event): Unit = {
    val ctrlEvent = read[ControlEvent](ev.content)
    ctrlEvent match {
      //does not make any sense now...
      case RegisteredClient(clientId, serverId) => println("registered as: " + id)
      case sync: ResyncDomain => resyncDomain(sync)
    }
  }

  private def reroute(msg: NodeMessage): Unit = {
    this.id onSuccess {
      case nodeId:Long =>{
        val routedMsg = msg.copy(route = msg.route :+ nodeId)
        this.connections.values.filter(p => !routedMsg.route.contains(p.targetId))
        .foreach(nc => {
        println("rerouted by:" + nodeId + " to: " + nc.targetId)
        nc.send(routedMsg)
      })
      }
    }
  }


  private def filterDomains(path: Seq[String]): Seq[Domain[_]] = {
    val res = this.domains
      .filter((v) => path.startsWith(v._1)).values.toSeq
    println(s"filtered domains:${res}")
    res
  }

  /**
   * Node receives message here.
   */
  def receiveMessage(msg: NodeMessage) = {
    receiveMessageLocal(msg)
    reroute(msg)
  }

  def receiveMessageLocal(msg: NodeMessage) = {
    println("received local message")
    messageListeners foreach (listener => listener.onMessage(msg))
    if (msg.destination.target == System) {
      processSysMessage(msg.event)
    } else {
      filterDomains(msg.destination.path).foreach((v) => sendEvenToDomain(msg.event, v))
    }
  }

  def saveEvent(event: Event, path: Seq[String]) = {
    domainStorages.get(path).foreach(store => store.saveEvent(event))
  }

  private def sendEvenToDomain(event: Event, domain: Domain[_]) = {
    println("passing event to domain:" + domain.path)
    val eventContext  =new NodeEventContext( this, event.sender)
    if (domain.receiveEvent(event, eventContext)) {
      if ( !event.transient) {
        saveEvent(event, domain.path)
      }
    }
  }

  def getDomainObject(path: Seq[String]) = {
    domains.get(path).get.getState()
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
        val event = Event(ControlEvent.writeEvent(ResyncDomain(nodeId, path, domain.recentEvents, syncBack)), 0, nodeId)
        val adr = Address(System, path)
        println("sending event:" + event)
        this.sendEvent(event, adr)
    }

  }
}
