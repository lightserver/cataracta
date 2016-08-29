package pl.setblack.lsa.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.setblack.lsa.events.Node
import pl.setblack.lsa.security.{KnownKey, SignedCertificate}

import scala.util.{Failure, Success}

class JVMServer(rootCertificate: SignedCertificate, knownKey: KnownKey) {

  def start() : Unit = {
    implicit val system = ActorSystem()
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val config = system.settings.config
    val interface = config.getString("app.interface")
    val port = config.getInt("app.port")
    val nodeId = config.getLong("app.node.id")

    val serverNode = new ServerSystem(nodeId, rootCertificate, knownKey)

    val service = new Webservice(serverNode )

    val binding = Http().bindAndHandle(service.route, interface, port)
    binding.onComplete {
      case Success(binding) ⇒
        val localAddress = binding.localAddress
        println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"Binding failed with ${e.getMessage}")
        system.shutdown()
    }
  }

}
