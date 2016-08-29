package pl.setblack.lsa.server

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import pl.setblack.lsa.events._


import upickle.default._


trait IncommingMessagesFlow {
  def theFlow(sender: String, token  :String): Flow[String, NodeMessage, akka.NotUsed]


}

object IncommingMessagesFlow {
  val secretToken = "buhaha"

  private def generateClientToken(clientId : Long): String = {
    val combined = s"${secretToken}/${clientId}"
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(combined.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

  private def checkClientToken(clientId : Long, token : String) : Boolean = {
    generateClientToken(clientId) == token
  }

  def create(system: ActorSystem, server : ServerSystem): IncommingMessagesFlow = {

    val boardActor:ActorRef =
      system.actorOf(Props(new Actor {
        var subscribers = Set.empty[ActorRef]

        def receive: Receive = {
          case NewParticipant(requestedID, token, subscriber) ⇒
            context.watch(subscriber)

            requestedID match {
              case "begForId" => {
                val clientId = server.nextClientNode
                val generatedToken = generateClientToken(clientId)
                subscribers += subscriber
                server.registerConnection( subscriber, clientId, generatedToken)
              }
              case x : String => {
                val id = x.toLong
                if ( checkClientToken(id, token) ) {
                  subscribers += subscriber
                  server.registerConnection( subscriber, id, token)
                } else {
                  //we ignore bad guy
                  println(s"ignoring bad connected guy ${id} ${token}")
                }
              }
            }

          //  subscriber ! NodeMessage( Address(System), Event( "test msg", 0,0))

          case NewNode(name, subscriber, clientId) =>

            server.registerActorConnection( subscriber, clientId)
            server.registeredRemoteActor(self, subscriber)
          case RegisteredNode(name ,subscriber, serverId) =>

            server.registerActorConnection( subscriber, serverId)
          case msg: ReceivedMessage    ⇒ {

            val nodeMessage = read[NodeMessageTransport](msg.message).toNodeMessage
            val routed = nodeMessage.copy( route = nodeMessage.route :+  nodeMessage.event.sender)

            server.receiveMessage(routed)

          }
          case ParticipantLeft(person) ⇒ println(s"{person} left")
          case Terminated(sub)         ⇒ {
            println(s"terminated ${sub}")
            subscribers -= sub
          }
        }

        def dispatch(msg: NodeMessage): Unit = {

          subscribers.foreach(_ ! msg)
        }
      }),name = "boardActor")

    def boardInSink(sender: String) = Sink.actorRef[BoardEvent](boardActor, ParticipantLeft(sender))
    server.registerToRemote( boardActor )
    server.resync()
    new IncommingMessagesFlow {
      def theFlow(sender: String, token : String): Flow[String, NodeMessage, akka.NotUsed] = {

        val in =
          Flow[String]
            .map(ReceivedMessage(sender,  _))
            .to(boardInSink(sender))
        val out =
          Source.actorRef[NodeMessage](1000, OverflowStrategy.dropHead)
            .mapMaterializedValue(boardActor ! NewParticipant(sender,  token, _))

        Flow.fromSinkAndSource(in, out)
      }

    }

  }


}

sealed trait BoardEvent
case class NewParticipant(name: String,  token : String, subscriber: ActorRef) extends BoardEvent
case class NewNode(name: String, subscriber: ActorRef, clientId: Long) extends BoardEvent
case class RegisteredNode(name: String, server: ActorRef, serverId: Long) extends BoardEvent
case class ParticipantLeft(name: String) extends BoardEvent
case class ReceivedMessage(sender: String, message: String) extends BoardEvent {
  //def toMessage: BoardMessage = BoardMessage(sender, read[BoardMessage](message).txt)
}
