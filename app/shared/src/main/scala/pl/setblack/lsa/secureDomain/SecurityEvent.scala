package pl.setblack.lsa.secureDomain

import pl.setblack.lsa.events.EventConverter
import pl.setblack.lsa.security.SignedCertificate

sealed trait SecurityEvent  {


}

//register known certificate
case class RegisterSignedCertificate(cert: SignedCertificate) extends SecurityEvent

//client should get this in order to proceed
case class RegisterSigner(authorToken: String, privateKey: String, publicKey: String, cert: SignedCertificate) extends SecurityEvent

object SecurityEvent {
  implicit object SecurityEventConverter extends EventConverter[SecurityEvent] {

    override def readEvent(str: String): SecurityEvent = upickle.default.read[SecurityEvent](str)

    override def writeEvent(e: SecurityEvent): String = upickle.default.write(e)
  }
}
