package pl.setblack.lsa.boot

import java.util.Date

import pl.setblack.lsa.events._
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.security.{SecurityProvider, SignedCertificate, SigningId}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

abstract class GenericSystem(val rootCertificate : Option[SignedCertificate])
                            (implicit executionContext: ExecutionContext) {

  val mainNode = createMainNode

  def createMainNode(): Node


  def load() = {
    this.mainNode.loadDomains()
  }

  def resync() = {
    this.mainNode.resync()
  }

  protected def createSecurityProvider: Future[SecurityProvider] = {
    val securityProvider = new SecurityProvider()
    rootCertificate.map(
      certificate => securityProvider
        .registerCertificate(certificate.info.author, certificate)
    ).getOrElse(Future {securityProvider})
  }

  def initSecurity() = {
    mainNode.initSecurityDomain()
  }
}



