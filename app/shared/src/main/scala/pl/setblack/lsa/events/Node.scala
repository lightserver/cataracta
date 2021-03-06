package pl.setblack.lsa.events

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.domains.DomainsManager
import pl.setblack.lsa.events.impl.{EventWrapper, _}
import pl.setblack.lsa.io.DomainStorage
import pl.setblack.lsa.os.Reality
import pl.setblack.lsa.secureDomain.SecurityEvent.SecurityEventConverter
import pl.setblack.lsa.secureDomain._
import pl.setblack.lsa.security.{RSAKeyPairExported, SecurityProvider, SigningId}
import slogging.{Logger, LoggerFactory, StrictLogging}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future, Promise}


/**
  * Node represents system to register domains and send pl.setblack.lsa.events.
  */
class Node(val id: Future[Long])(
  implicit val realityConnection: Reality
) extends StrictLogging {

  type InternalDomainRef = BadActorRef[EventWrapper]

  type NodeRef = BadActorRef[NodeEvent]

  import ExecutionContext.Implicits.global

  val nodeRef:NodeRef  = realityConnection.concurrency.createSimpleActor(new NodeActor(this))

  //node
  private var domainsManager = new DomainsManager

  private val eventSequencer = new EventSequencer

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
    registerDomain(SecurityDomain.securityDomainPath, securityDomain)
    registerDomain(PrivateSecurityDomain.privateSecurityDomainPath, privateSecurityDomain)
  }

  //Jarek:domains  should load themselves
  def loadDomains() = {
    this.domainsManager.loadDomains()
  }

  def hasDomain(path: Seq[String]): Boolean = this.domainsManager.hasDomain(path)

  def registerMessageListener(listener: MessageListener): Unit = {
    this.domainsManager = domainsManager.registerMessageListener(listener)
  }

  def registerDomain[O, EVENT](path: Seq[String], domain: Domain[O, EVENT]) = {
    val actor = new DomainActor(domain, new DomainStorage(path, realityConnection.storage), nodeRef, path)
    val domainRef: BadActorRef[EventWrapper] = realityConnection.concurrency.createSimpleActor(actor)
    domainsManager = domainsManager.withDomain(path, domainRef)

    new DomainRef[EVENT](
      path,
      nodeRef
    )(domain.eventConverter)
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
        this.eventSequencer.nextEventId.onSuccess {
          case evId: Long => {
            val event = new UnsignedEvent(content, evId, nodeid)
            this.sendEvent(event, adr)
          }
        }
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
        this.eventSequencer.nextEventId.onSuccess {
          case eventId: Long => {
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
        println("jest sukces")
        println(s"mam id=${nodeId}")
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

  private[events] def registerDomainListener[O, X](listener: DomainListener[O, X], path: Seq[String]): Unit = {
    domainsManager.filterDomains(path).foreach(d => d._2.send(RegisterListener[O, X](listener)))
  }

  private def registerDomainListener[O, X](listener: DomainListener[O, X], ref: DomainRef[_]): Unit = {
    this.registerDomainListener(listener, ref.path)
  }

  private def resyncDomain(sync: ResyncDomain, connectionData: ConnectionData): Unit = {
    domainsManager.filterDomains(sync.domain).map(
      domainRef => {
        this.id onSuccess { case nodeId =>
          domainRef._2.send(new ResyncDomainCommand(sync, nodeId))
        }
      }
    )

  }

  private def restoreDomain(serialized: RestoreDomain): Unit = {
    domainsManager.filterDomains(serialized.domain).foreach(domainRef => {
      domainRef._2.send(new RestoreDomainCommand(serialized))
    })
  }

  private[events] def restoreDomain(path: Seq[String]): Unit = {
    this.eventSequencer.block()
    this.id onSuccess { case nodeId: Long =>
      domainsManager.filterDomains(path).foreach(domainRef => {

        domainRef._2.send(LoadDomainCommand)
        domainRef._2.send(SyncDomainCommand(nodeId, true))
      })
    }
  }

  private[events] def deblock(maxEvenID: Long): Unit = {
    this.eventSequencer.deblockAt(maxEvenID)
  }


  def listenDomains(listen: ListenDomains, sender: Long) = {
    this.connections.get(sender).foreach(nodeConnection => nodeConnection.setListeningTo(listen.domains))
  }

  def processSysMessage(ev: Event, ctx: Address => EventContext): Unit = {

    val ctrlEvent = read[ControlEvent](ev.content)
    this.id onSuccess {
      case nodeId: Long =>
        if (ev.sender != nodeId) {
          ctrlEvent match {
            //does not make any sense now...
            case RegisteredClient(clientId, serverId, token) => println("registered as: " + id)
            case sync: ResyncDomain => resyncDomain(sync, ctx(Address(System)).connectionData)
            case serialized: RestoreDomain => restoreDomain(serialized)
            case listen: ListenDomains => listenDomains(listen, ev.sender)
            case Ping => {
              logger.debug("got ping")
            }
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
    val ctx = ( adr:Address ) => new NodeEventContext(
      this, msg.event.sender, adr, connectionData, None)
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
        val ctx = (adr:Address) => new NodeEventContext(this, signed.sender, adr, connectionData, Some(x))
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

  /** TODO: make private and change to actor receive */
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

/** TODO: make private and change to actor receive */
  def receiveMessageLocal(msg: NodeMessage, ctx: Address => EventContext) = {
    if (msg.destination.target == System) {
      processSysMessage(msg.event, ctx)
    } else {
      logger.debug(s"Received event[${msg.event.id}]")
      domainsManager.receiveLocalDomainMessage(msg, ctx, this.id)
    }
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
