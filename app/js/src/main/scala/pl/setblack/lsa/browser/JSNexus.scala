package pl.setblack.lsa.browser

import pl.setblack.lsa.events.Node
import pl.setblack.lsa.security.SignedCertificate

import scala.concurrent.{ExecutionContext, Promise}

class JSNexus(rootCertificate: Option[SignedCertificate] = None)
             (implicit val executionContext: ExecutionContext){

  def start(): Node = {
    val clientId = Promise[Long]()

    val clientSystem = new JSSystem(clientId.future, rootCertificate)
        println("initializing")
        val connection = new ServerConnection(clientSystem.mainNode, clientId)
        clientSystem.mainNode
    }

}
