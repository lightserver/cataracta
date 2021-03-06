package pl.setblack.lsa.server

import akka.actor.{ActorRef, ActorSystem}
import pl.setblack.lsa.boot.GenericSystem
import pl.setblack.lsa.events.{ConnectionData, Node, NodeMessage}
import pl.setblack.lsa.security.{KnownKey, SecurityProvider, SignedCertificate}
import slogging.StrictLogging

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

class ServerSystem(nodeId: Long,
                   initialRootCertificate: Option[SignedCertificate],
                   initialRootKey: Option[KnownKey]
                  )(implicit system: ActorSystem, executionContext: ExecutionContext)
  extends GenericSystem(initialRootCertificate)
    with StrictLogging {

  var nextClientNodeId: Long = 2048 * nodeId + scala.util.Random.nextInt(1024)
  val connectionData = mutable.Map[Long, ConnectionData]()

  def nextClientNode: Long = {
    nextClientNodeId = nextClientNodeId + 1
    nextClientNodeId
  }

  override def createMainNode(): Node = {
    val reality = JVMRealityConnection.create(createSecurityProvider)
    val node = new Node(Promise[Long].success(nodeId).future)(reality)
    node
  }

  def receiveMessage(msg: NodeMessage): Unit = {
    println(s"received message ${msg}")
    mainNode.receiveMessage(msg,
      this.connectionData.getOrElseUpdate(msg.event.sender, new ConnectionData()))
  }

  def registerConnection(subscriber: ActorRef, clientId: Long, token: String) = {
    logger.debug(s"registering connection from ${clientId}")
    mainNode.registerConnection(clientId, Some(token), new ServerWSProtocol(subscriber))
  }

  def registerActorConnection(subscriber: ActorRef, clientId: Long) = {
    val connection = mainNode.registerConnection(clientId, None, new ServerRemoteProtocol(subscriber, clientId.toString))
    resync()
  }

  def registeredRemoteActor(board: ActorRef, subscriber: ActorRef): Unit = {
    subscriber ! RegisteredNode("server", board, nodeId)
  }

  def registerToRemoteActor(board: ActorRef, remotePath: String): Unit = {
    val actorRef = system.actorSelection(remotePath)
    actorRef ! NewNode("server", board, nodeId)
  }

  def registerToRemote(theBoard: ActorRef): Unit = {
    if (system.settings.config.hasPath("app.node.connectTo")) {

      val remoteActors = system.settings.config.getStringList("app.node.connectTo")

      val remoteSystems: Seq[String] = scala.collection.JavaConversions.asScalaBuffer(remoteActors).toSeq

      remoteSystems.foreach(remotePath => registerToRemoteActor(theBoard, remotePath))
    }
  }

  override protected def createSecurityProvider: Future[SecurityProvider] = {
    val initialSecurityProvider = super.createSecurityProvider
    for {
      provider <- initialSecurityProvider
      nextProvider <- (
          for {
            certificate <- initialRootCertificate
            key <- initialRootKey
          } yield provider.registerSigner(key.author, key.exported, certificate)
          ).getOrElse(initialSecurityProvider)
    }  yield nextProvider
  }

}

