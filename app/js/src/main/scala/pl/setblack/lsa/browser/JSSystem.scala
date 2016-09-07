package pl.setblack.lsa.browser

import pl.setblack.lsa.boot.GenericSystem
import pl.setblack.lsa.concurrency.NoConcurrencySystem
import pl.setblack.lsa.events.Node
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.os.SimpleReality
import pl.setblack.lsa.resources.JSResources
import pl.setblack.lsa.security.SignedCertificate

import scala.concurrent.{Future, Promise}

class JSSystem (val clientId: Future[Long],rootCertificate : SignedCertificate)  extends GenericSystem(rootCertificate){
  import scala.concurrent.ExecutionContext.Implicits.global


  override def createMainNode(): Node = {

    implicit  val reality = SimpleReality(createStorage, new NoConcurrencySystem, createSecurityProvider, new JSResources())
    val node = new Node(clientId)(reality)
    node
  }

  private def createStorage(): Storage = {
    class NullStorage extends Storage {
      override def save(value: String, path: Seq[String]): Unit = {}

      override def load(path: Seq[String]): Option[String] = None
    }
    return new NullStorage
  }
}
