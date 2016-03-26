package pl.setblack.lsa.security

import pl.setblack.lsa.cryptotpyrc.UniCrypto
import pl.setblack.lsa.cryptotpyrc.rsa.RSAPrivateKey

import scala.concurrent.Future

class Signer( val privateKey : RSAPrivateKey, val myCertificate : SignedCertificate ) {
   import scala.concurrent.ExecutionContext.Implicits.global
    val rsa = new UniCrypto().rsa

    def sign(msg : String) : Future[MessageSignature] = {
        rsa.sign(privateKey, msg).map(
          signedMsg => MessageSignature(signedMsg, myCertificate.info)
        )
    }
}
