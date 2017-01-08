package pl.setblack.lsa.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.stage._
import pl.setblack.lsa.boot.GenericSystem
import pl.setblack.lsa.events.NodeMessage
import pl.setblack.lsa.security.SignedCertificate
import upickle.default._

import scala.concurrent.{Future, Promise}

class Webservice(serverNode: ServerSystem)(implicit fm: Materializer, system: ActorSystem) extends Directives {


  val incommingMessages = IncommingMessagesFlow.create(system, serverNode)


  def route =
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        // Scala-JS puts them in the root of the resource directory per default,
        // so that's where we pick them up
        pathPrefix("scjs")(getFromResourceDirectory("")) ~
        pathPrefix("vidi") (getFromResource("web/index.html") )~
        pathPrefix("services") {
          path("board") {
            parameters('id, 'token) { (id: String, token: String) ⇒ {
              handleWebSocketMessages(websocketMessagesFlow(id, token))
            }

            }
          } ~ {
            complete {
              "ok normalnie"
            }
          }
        }
    } ~
      getFromResourceDirectory("web")


  def websocketMessagesFlow(clientId: String, token: String): Flow[Message, Message, akka.NotUsed] = {

    Flow[Message].mapAsync(3) {

      case ts: TextMessage => {
        val source = ts.textStream
        val sink = Sink.fold[String, String]("")(_ + _)
        val sum: Future[String] = source.runWith(sink)
        sum

      }
      case x: Any => {
        Promise[String].success("bad val").future
      }
    }.via(incommingMessages.theFlow(clientId, token))
      .map {
        case msg@NodeMessage(adr, event, _) => {

          TextMessage.Strict(write(msg.toTransport))
        }
        case _ => {

          TextMessage.Strict(write("unknown message encountered"))
        }
      }.via(reportErrorsFlow)
  }


  def reportErrorsFlow[T]: Flow[T, T, akka.NotUsed] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream failed with $cause")
          cause.printStackTrace()
          super.onUpstreamFailure(cause, ctx)
        }
      })
}


