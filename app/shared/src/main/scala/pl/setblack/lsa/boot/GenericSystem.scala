package pl.setblack.lsa.boot

import java.util.Date

import pl.setblack.lsa.events._
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.security.{SecurityProvider, SignedCertificate, SigningId}
import upickle.default._

import scala.concurrent.Future

abstract class GenericSystem(val rootCertificate : SignedCertificate) {

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
    val certificate = rootCertificate
    securityProvider
      .registerCertificate(certificate.info.author, certificate)
  }

  def initSecurity() = {
    mainNode.initSecurityDomain()
  }
}



