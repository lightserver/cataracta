package pl.setblack.lsa.security

import scala.concurrent.Future

trait Verifier {
  def addTrusted( certificate : CertificateInfo) : Verifier

  def verify( message : String, signature : MessageSignature) : Future[Boolean]

}
