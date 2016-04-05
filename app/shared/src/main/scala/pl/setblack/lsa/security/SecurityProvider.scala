package pl.setblack.lsa.security

import pl.setblack.lsa.cryptotpyrc.rsa.{RSAPrivateKey, RSAPublicKey}
import pl.setblack.lsa.cryptotpyrc.{KeyPair, CryptoAlg, UniCrypto}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class SecurityProvider(
                             rsa: CryptoAlg[RSAPublicKey, RSAPrivateKey] = new UniCrypto().rsa,
                             trustedRoot: Map[SigningId, SignedCertificate] = Map(),
                             publicKeys: Map[SigningId, RSAPublicKey] = Map(),
                             privateKeys: Map[SigningId, RSAPrivateKey] = Map(),
                             certificates: Map[SigningId, SignedCertificate] = Map()
                           ) {

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
            val certificateInfo = CertificateInfo(hash, ca)
            rsa.importPrivate(privateKey).flatMap(
              importedPrivKey => {
                rsa.sign(importedPrivKey, certificateInfo.toString).map({
                  signedCertificate => {
                    val signature = MessageSignature(signedCertificate, certificateInfo)
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

  def generateKeyPair(signer: SigningId, signedBy: SigningId): Future[(RSAKeyPairExported, SignedCertificate, SecurityProvider)] = {
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
      pubKeyHash <- {
        rsa.digest(publicKey)
      }
      exportedKeyPair = RSAKeyPairExported(publicKey, privateKey)
      withSignedCert <- this.signCertificate(signedBy, CertificateInfo(pubKeyHash, signer))
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
      val signedCert = SignedCertificate(cert, ms)
      (this.addCertificate(cert.author, signedCert), signedCert)
    })
  }


  def isValidSignature(signature: MessageSignature, signedString: String): Future[Boolean] = {
    val authorId = signature.signedBy.author
    (for {
    //signedLocalCertificate <- this.certificates.get(authorId)
      localPublicKey <- {
        val pub = this.publicKeys.get(authorId)
        pub.foreach( k => k.export onSuccess {
          case exported => println(s"exported pub is ${exported}")
        })
        pub
      }
    } yield (rsa.verify(localPublicKey, signature.signedString, signedString)))
      .getOrElse(Future {
        false
      })

  }

  def signAs(author: SigningId, message: String): Future[MessageSignature] = {
    val privateKey = this.privateKeys(author)
    val certificate = this.certificates(author)
    for {
      signature <- {
        this.rsa.sign(privateKey, message)
      }
    } yield ({
      MessageSignature(signature, certificate.info)
    })
  }

  private def importKeys(privateKey: String, publicKey: String)
  : Future[RSAKeyPair] = {
    val result = rsa.importPublic(publicKey).flatMap(importedPublicKey => {
      rsa.importPrivate(privateKey).map(importedPrivateKey => {
        new RSAKeyPair(importedPublicKey, importedPrivateKey)
      })
    })
    result onFailure  {
      case e => {
        println("failed import")
        e.printStackTrace()
      }
    }
    result
  }
}

case class RSAKeyPairExported(publicKey: String, privateKey: String)

