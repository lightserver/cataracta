package pl.setblack.lsa.events


import pl.setblack.lsa.concurrency.{BadActorRef, ConcurrencySystem}
import pl.setblack.lsa.events.domains.DomainsManager
import pl.setblack.lsa.events.impl.{EventWrapper, _}
import pl.setblack.lsa.io.{DomainStorage, Storage}
import pl.setblack.lsa.os.Reality
import pl.setblack.lsa.secureDomain._
import pl.setblack.lsa.security.{RSAKeyPairExported, SecurityProvider, SigningId}
import slogging.StrictLogging
import upickle.default._

import scala.concurrent.{ExecutionContext, Future, Promise}


/**
  * Node represents system to register domains and send pl.setblack.lsa.events.
  */
class Node(val id: Future[Long])(
  implicit val realityConnection: Reality
) extends StrictLogging {
  type InternalDomainRef = BadActorRef[EventWrapper]

  import ExecutionContext.Implicits.global

  val nodeRef: BadActorRef[NodeEvent] = realityConnection.concurrency.createSimpleActor(new NodeActor(this))

  //node
  private var domainsManager = new DomainsManager

  private var nextEventId: Long = 0

  //dispatcher
  private val connections = new scala.collection.mutable.HashMap[Long, NodeConnection]()

  private val loopConnection = registerConnection(id, new LoopInvocation(this))

  //security
  private var security: Future[SecurityProvider] = realityConnection.security

  //---node ---
  def this(constId: Long)(implicit reality: Reality) = {
    this(Promise[Long].success(constId).future)(reality)
  }

  //Jarek: evacuate it somewhere
  def initSecurityDomain() = {
    val securityDomain = new SecurityDomain("nic", nodeRef)
    val privateSecurityDomain = new PrivateSecurityDomain("nic", nodeRef)
    registerDomain(securityDomain.path, securityDomain)
    registerDomain(privateSecurityDomain.path, privateSecurityDomain)
  }

  //Jarek:domains  should load themselves
  def loadDomains() = {
    this.domainsManager.loadDomains()
  }

  def hasDomain(path: Seq[String]): Boolean = this.domainsManager.hasDomain(path)

  def registerMessageListener(listener: MessageListener): Unit = {
    this.domainsManager = domainsManager.registerMessageListener(listener)
  }

  def registerDomain[O](path: Seq[String], domain: Domain[O]) = {
    val actor = new DomainActor(domain, new DomainStorage(path, realityConnection.storage), nodeRef)
    val domainRef: BadActorRef[EventWrapper] = realityConnection.concurrency.createSimpleActor(actor)
    domainsManager = domainsManager.withDomain(path, domainRef)

    new DomainRef[domain.EVENT](path, domain.getEventConverter, nodeRef)
  }


  def resync(): Unit = {
    this.id onSuccess {
      case nodeId => this.domainsManager.resync(nodeId)
    }
  }


  //dispatcher ---------------------------------------------------------------------------------------------------------------------

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
        val event = new UnsignedEvent(content, getNextEventId(), nodeid)
        this.sendEvent(event, adr)
      }
    }
  }

  private def makeSignedString(eventContent: String, id: Long, nodeId: Long, adr: Address) = {
    s"${eventContent}:${id}:${nodeId}"
  }

  def sendSignedEvent(content: String, adr: Address, author: SigningId): Unit = {
    this.id.onSuccess {
      case nodeid: Long => {
        logger.debug(s"send sign event ${content} with [${security}]")
        val eventId = getNextEventId()
        val toSignMessage = makeSignedString(content, eventId, nodeid, adr)
        (for {
          signature <- {
            logger.debug(s"signing msg ${toSignMessage}")
            security.flatMap(_.signAs(author, toSignMessage))
          }
        } yield {
          val event = new SignedEvent(content, eventId, nodeid, signature)
          this.sendEvent(event, adr)
        }) onFailure {
          case e => {
            logger.error("unable to sign event", e)
            e.printStackTrace()
          }
        }

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
        result.success(this.connections.values.toSeq)
      }
      case System => result.success(this.connections.values.toSeq)
      case Target(x) => result.success(this.connections.values.filter(node => node.knows(x)).toSeq)
      case _ => println(s" I got some crazy target ${adr.target}")
    }

    result.future
  }

  private def createClientIdMessage(clientId: Long, token: String): Future[NodeMessage] = {
    this.id.map {
      case nodeId: Long => {
        val event = new UnsignedEvent(write[ControlEvent](RegisteredClient(clientId, nodeId, token)), 1, nodeId)
        NodeMessage(Address(System), event)
      }
    }
  }


  def registerConnection(id: Long, token: Option[String], protocol: Protocol): Future[NodeConnection] = {
    val connection = registerConnection(Promise[Long].success(id).future, protocol)
    for {
      clientToken <- token
      controlMessage = this.createClientIdMessage(id, clientToken)
      vc <- connection
      msg <- controlMessage
    } vc.send(msg)

    connection
  }


  def registerConnection(futureId: Future[Long], protocol: Protocol): Future[NodeConnection] = {
    val futureConnection = Promise[NodeConnection]
    futureId onSuccess {
      case nodeId: Long => {
        val connection = new NodeConnection(nodeId, protocol)
        this.connections += (nodeId -> connection)
        futureConnection.success(connection)
      }
    }
    futureConnection.future
  }

  def getConnections(): Map[Long, NodeConnection] = {
    this.connections.toMap
  }

  def registerDomainListener[O, X](listener: DomainListener[O, X], path: Seq[String]): Unit = {
    domainsManager.filterDomains(path).foreach(d => d.send(RegisterListener[O, X](listener)))
  }

  private def resyncDomain(sync: ResyncDomain, connectionData: ConnectionData): Unit = {
    domainsManager.filterDomains(sync.domain).map(
      domainRef => {
        this.id onSuccess { case nodeId =>
          domainRef.send(new ResyncDomainCommand(sync, nodeId))
        }
      }
    )

  }

  private def restoreDomain(serialized: RestoreDomain): Unit = {
    domainsManager.filterDomains(serialized.domain).foreach(domainRef => {
      domainRef.send(new RestoreDomainCommand(serialized))
    })
  }


  def listenDomains(listen: ListenDomains, sender: Long) = {
    this.connections.get(sender).foreach(nodeConnection => nodeConnection.setListeningTo(listen.domains))
  }

  def processSysMessage(ev: Event, ctx: EventContext): Unit = {

    val ctrlEvent = read[ControlEvent](ev.content)
    this.id onSuccess {
      case nodeId: Long =>
        if (ev.sender != nodeId) {
          ctrlEvent match {
            //does not make any sense now...
            case RegisteredClient(clientId, serverId, token) => println("registered as: " + id)
            case sync: ResyncDomain => resyncDomain(sync, ctx.connectionData)
            case serialized: RestoreDomain => restoreDomain(serialized)
            case listen: ListenDomains => listenDomains(listen, ev.sender)
            case Ping => {}
          }
        }
    }

  }

  private def dumoConnections() = {
    this.connections.foreach((mapEntry) => {
      println(s"${mapEntry._1} -> ${mapEntry._2.getClass}")
    })
  }

  private def reroute(msg: NodeMessage): Unit = {
    dumoConnections()
    this.id onSuccess {
      case nodeId: Long => {
        val routedMsg = msg.copy(route = msg.route :+ nodeId)
        this.getConnectionsForAddress(msg.destination).foreach(
          dstCollection => dstCollection.filter(p => !routedMsg.route.contains(p.targetId)).foreach(nc => {
            nc.send(routedMsg)
          })
        )
      }
    }
  }


  /**
    * Node receives message here.
    */
  private def receiveMessageUnsigned(msg: NodeMessage, connectionData: ConnectionData) = {
    val ctx = new NodeEventContext(this, msg.event.sender, connectionData, None)
    receiveMessageLocal(msg, ctx)
    rerouteMsg(msg)
  }

  private def rerouteMsg(msg: NodeMessage): Unit = {
    msg.destination.target match {
      case All => reroute(msg)
      case Target(x) => reroute(msg)
      case _ =>
    }
  }

  def receiveMessagSigned(msg: NodeMessage, signed: SignedEvent, connectionData: ConnectionData) = {

    this.security = this.security.flatMap(secInstance => {
      secInstance.isValidSignature(signed.signature,
        makeSignedString(signed.content, signed.id, signed.sender, msg.destination))
    }).map {
      case (newSec, Some(x)) => {
        val ctx = new NodeEventContext(this, signed.sender, connectionData, Some(x))
        receiveMessageLocal(msg, ctx)
        rerouteMsg(msg)
        newSec
      }
      case (newSec, None) => {
        logger.warn(s"signature failed for message [${msg.event.content}]")
        newSec
      }
    }


  }

  def receiveMessage(msg: NodeMessage, connectionData: ConnectionData) = {
    msg.event match {
      case unsigned: UnsignedEvent => {
        receiveMessageUnsigned(msg, connectionData)
      }
      case signed: SignedEvent => {
        receiveMessagSigned(msg, signed, connectionData)
      }
    }
  }


  def receiveMessageLocal(msg: NodeMessage, ctx: EventContext) = {
    if (msg.destination.target == System) {
      processSysMessage(msg.event, ctx)
    } else {
      domainsManager.receiveLocalDomainMessage(msg, ctx)
    }
  }


  def getNextEventId(): Long = {
    this.nextEventId += 1
    this.nextEventId
  }


  def ping() = {
    val adr = Address(System)
    val pingEvent = ControlEvent.writeEvent(Ping)
    this.sendEvent(pingEvent, adr)
  }

  def generateKeyPair(author: SigningId, certifiedBy: SigningId, privileges: Set[String], adr: Address): Unit = {

    val generated = security.flatMap(secInstance => {
      secInstance.generateKeyPair(author, certifiedBy, privileges)
    })
    security = generated.map(_._3)

    generated.foreach(gen => {
      val kp = gen._1
      val cert = gen._2
      val privKeyEvent = RegisterSigner(author.authorId, kp.privateKey, kp.publicKey, cert)
      this.sendEvent(SecurityEventConverter.writeEvent(privKeyEvent), adr)
      val certificateEvent = RegisterSignedCertificate(cert)
      this.sendEvent(
        SecurityEventConverter.writeEvent(certificateEvent),
        Address(path = SecurityDomain.securityDomainPath)
      )

    })
    generated.onFailure {
      case e => e.printStackTrace()
    }

  }

  def registerSigner(regSigner: SecRegisterSigner): Unit = {
    val event = regSigner.secEvent
    this.security = for {

      securityProviderWithSigner <- security.flatMap(_.registerSigner(SigningId(event.authorToken),
        RSAKeyPairExported(event.publicKey, event.privateKey),
        event.cert))
    } yield securityProviderWithSigner
  }

  case class DomainEvent(event: Event, ctx: EventContext)

}
