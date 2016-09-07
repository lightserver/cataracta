package pl.setblack.lsa.browser

import pl.setblack.lsa.boot.GenericSystem
import pl.setblack.lsa.concurrency.NoConcurrencySystem
import pl.setblack.lsa.events.Node
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.os.SimpleReality
import pl.setblack.lsa.resources.JSResources
import pl.setblack.lsa.security.{SecurityProvider, SignedCertificate}

import scala.concurrent.{Future, Promise}

class JSBeeing(rootCertificate: SignedCertificate) {
  import scala.concurrent.ExecutionContext.Implicits.global

  def start(): Unit = {
    val clientId = Promise[Long]()

    val clientSystem = new JSSystem(clientId.future, rootCertificate)



        println("initializing")
        val connection = new ServerConnection(clientSystem.mainNode, clientId)

    }

}
