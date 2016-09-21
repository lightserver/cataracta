package pl.setblack.lsa.security

import pl.setblack.lsa.cryptotpyrc.rsa.jvm.RSACryptoAlg
import upickle.default._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object KeyGenerator {

  import scala.concurrent.ExecutionContext.Implicits.global

  def main(s : Array[String]):Unit = {
    generateKeys()
  }

  private def generateKeys(): Unit = {
    val rsa = new RSACryptoAlg
    val keys  = rsa.generateKeys()
    val selfSigned:Future[(RSAKeyPairExported, SignedCertificate)] = for {
      keys <- rsa.generateKeys()
      exportedPublic <- keys.pub.export
      exportedPrivate <- keys.priv.export
      certInfo = CertificateInfo(exportedPublic, SigningId("root"), Set("all"),"2099-10-09")
      signedCert <- rsa.sign(keys.priv, certInfo.toString())
      signature  = CertificateSignature(signedCert, certInfo)
    } yield ( RSAKeyPairExported(exportedPublic,exportedPrivate), SignedCertificate(certInfo, signature ))
    Await.ready(selfSigned, Duration.Inf)
    println( write(selfSigned.value.get.get._1))
    println( write(selfSigned.value.get.get._2))
  }

}
