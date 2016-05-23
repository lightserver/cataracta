package pl.setblack.lsa.security

import java.time.{Instant, LocalDateTime}

import pl.setblack.lsa.cryptotpyrc.rsa.{RSAPrivateKey, RSAPublicKey}
import pl.setblack.lsa.cryptotpyrc.{CryptoAlg, KeyPair, UniCrypto}
import slogging.StrictLogging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class SecurityProvider(
                             rsa: CryptoAlg[RSAPublicKey, RSAPrivateKey] = new UniCrypto().rsa,
                             trustedRoot: Map[SigningId, SignedCertificate] = Map(),
                             publicKeys: Map[SigningId, RSAPublicKey] = Map(),
                             privateKeys: Map[SigningId, RSAPrivateKey] = Map(),
                             certificates: Map[SigningId, SignedCertificate] = Map()
                           ) extends StrictLogging{

  type RSAKeyPair = KeyPair[RSAPublicKey, RSAPrivateKey]

  private def addRootCA(ca: SigningId, cert: SignedCertificate): SecurityProvider = {
    this.copy(trustedRoot = trustedRoot + (ca -> cert))
  }

  private def addPublicKey(ca: SigningId, key: RSAPublicKey): SecurityProvider = {
    this.copy(publicKeys = publicKeys + (ca -> key))
  }

  private def addPrivateKey(ca: SigningId, key: RSAPrivateKey): SecurityProvider = {
    this.copy(privateKeys = privateKeys + (ca -> key))
  }

  private def addCertificate(signer: SigningId, signedCert: SignedCertificate): SecurityProvider = {
    this.copy(certificates = certificates + (signer -> signedCert))
  }

  //there may be more root CA
  def registerRootCA(ca: SigningId, publicKey: String, privateKey: String): Future[SecurityProvider] = {
    rsa.importPublic(publicKey).flatMap(
      importedPublicKey => {
        rsa.digest(publicKey).flatMap(
          hash => {
            val certificateInfo = CertificateInfo(hash, ca, Set(), "2100-12-06T10:15:30.00Z")
            rsa.importPrivate(privateKey).flatMap(
              importedPrivKey => {
                rsa.sign(importedPrivKey, certificateInfo.toString).map({
                  signedCertificate => {
                    val signature = CertificateSignature(signedCertificate, certificateInfo)
                    val signedCert = SignedCertificate(certificateInfo, signature)
                    signedCert
                  }
                }).map(selfSigned => {
                  this.addRootCA(ca, selfSigned)
                    .addPublicKey(ca, importedPublicKey)
                })
              }
            )
          }
        )
      }
    )
  }

  def generateKeyPair(signer: SigningId, signedBy: SigningId, privileges: Set[String]): Future[(RSAKeyPairExported, SignedCertificate, SecurityProvider)] = {
    for {
      keyPair: KeyPair[RSAPublicKey, RSAPrivateKey] <- {
        val keyPairOpt = privateKeys.get(signer).flatMap(internalPriv => publicKeys.get(signer).map(internalPub => Future {
          KeyPair(internalPub, internalPriv)
        }
        ))
        val result: Future[KeyPair[RSAPublicKey, RSAPrivateKey]] = keyPairOpt.getOrElse({
          rsa.generateKeys()
        })
        result
      }
      publicKey <- {
        keyPair.pub.export
      }
      privateKey <- {
        keyPair.priv.export
      }
      /*pubKeyHash <- {
      rsa.digest(publicKey)
    }*/
      exportedKeyPair = RSAKeyPairExported(publicKey, privateKey)
      withSignedCert <- this.signCertificate(signedBy,
        CertificateInfo(publicKey, signer, privileges, "2100-12-06T10:15:30.00Z"))
      withKey <- withSignedCert._1.registerSigner(signer, exportedKeyPair, withSignedCert._2)
    } yield (exportedKeyPair,
      withSignedCert._2,
      withKey)
  }

  def registerSigner(signer: SigningId,
                     exportedKey: RSAKeyPairExported,
                     signedCert: SignedCertificate): Future[SecurityProvider] = {
    importKeys(exportedKey.privateKey, exportedKey.publicKey).map((pair) => {
      this.addPrivateKey(signer, pair.priv)
        .addPublicKey(signer, pair.pub)
        .addCertificate(signer, signedCert)
    })
  }

  def registerCertificate(author: SigningId, signedCert: SignedCertificate): SecurityProvider = {
    this.addCertificate(author, signedCert)
  }

  def signCertificate(signer: SigningId, cert: CertificateInfo): Future[(SecurityProvider, SignedCertificate)] = {
    signAs(signer, cert.toString).map(ms => {
      val signedCert = SignedCertificate(cert, CertificateSignature(ms.signedString, cert))
      (this.addCertificate(cert.author, signedCert), signedCert)
    })
  }


  def isValidSignature(signature: MessageSignature, message: String): Future[(SecurityProvider, Option[CertificateInfo])] = {
    val authorId = signature.signedBy.info.author
    val publicKey = this.publicKeys.get(authorId)
    //if there is public key  - simply check with
    val knownCertificateOption = publicKey.map(pub => rsa.verify(pub, signature.signedString, message))
      .map(fVerified => fVerified.map(v => {
        if (v) Some(signature.signedBy) else None
      }
      ))
    val result:Future[(SecurityProvider, Option[CertificateInfo])] = knownCertificateOption match {
      case Some(x) => x.map(signedCert => (this,signedCert.map(sc => sc.info)))
      case None =>  {
        val messagePublicKey = signature.signedBy.info.publKey
        val authorityPublicKey = this.publicKeys.get(signature.signedBy.signature.signedBy.author)
        val verificationOfSignature: Option[Future[Boolean]] = authorityPublicKey.map(key => rsa.verify(key,
          signature.signedBy.signature.signedString,
          signature.signedBy.info.toString))
        verificationOfSignature match {
          case Some(future) => {
            future.flatMap( verificationPositive => if ( verificationPositive){
              val rsaFuture = rsa.importPublic(messagePublicKey)
              val futureSc:Future[SecurityProvider] = rsaFuture.map( imported => this.addPublicKey(authorId, imported))
              futureSc.flatMap(fs => isValidSignature(signature, message))
            } else {
              Future {(this, None)}
            })
          }
          case None => {
            println("trusted authority is unnown  --sorry")
            Future {(this, None)}
          }
        }

      }
      //we have to verify certificate first

    }

    result
  }

  private def verifyCertificate(cerInfo: CertificateInfo): Future[Option[CertificateInfo]] = {
    Future {
      None
    }
  }

  def signAs(author: SigningId, message: String): Future[MessageSignature] = {
    val privateKey = this.privateKeys(author)
    val certificate = this.certificates(author)
    for {
      signature <- {
        logger.debug(s"signing ${message}")
        this.rsa.sign(privateKey, message)
      }
    } yield ({
      MessageSignature(signature, certificate)
    })
  }

  private def importKeys(privateKey: String, publicKey: String)
  : Future[RSAKeyPair] = {
    val result = rsa.importPublic(publicKey).flatMap(importedPublicKey => {
      rsa.importPrivate(privateKey).map(importedPrivateKey => {
        new RSAKeyPair(importedPublicKey, importedPrivateKey)
      })
    })
    result onFailure {
      case e => {
        println("failed import")
        e.printStackTrace()
      }
    }
    result
  }
}

case class RSAKeyPairExported(publicKey: String, privateKey: String)

