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
    println(s"added pub key for ${ca}")
    this.copy(publicKeys = publicKeys + (ca -> key))
  }

  private def addPrivateKey(ca: SigningId, key: RSAPrivateKey): SecurityProvider = {
    println(s"added priv key for ${ca}")
    this.copy(privateKeys = privateKeys + (ca -> key))
  }

  private def addCertificate(signer: SigningId, signedCert: SignedCertificate): SecurityProvider = {
    println(s"added cert key for ${signer}")
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
    println("generating key pair in provider....")
    for {
      keyPair: KeyPair[RSAPublicKey, RSAPrivateKey] <- {
        println("@keyPair")
        val keyPairOpt = privateKeys.get(signer).flatMap(internalPriv => publicKeys.get(signer).map(internalPub => Future {
          KeyPair(internalPub, internalPriv)
        }
        ))
        val result: Future[KeyPair[RSAPublicKey, RSAPrivateKey]] = keyPairOpt.getOrElse({
          println("realy generating...")
          rsa.generateKeys()
        })
        result
      }
      publicKey <- {
        println("@pub Export")
        keyPair.pub.export
      }
      privateKey <- {
        println("@priv Export")
        keyPair.priv.export
      }
      pubKeyHash <- {
        println("@digest")
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
    println(s"rgistering signer ${signer}")
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
    println(s"validating message[${signedString}]")
    println(s"validating signature[${signature.signedString}]")
    val authorId = signature.signedBy.author
    println(s"validating for author[${authorId}]")
    (for {
    //signedLocalCertificate <- this.certificates.get(authorId)
      localPublicKey <- {
        val pub = this.publicKeys.get(authorId)
        println(s"found public key ${pub}")
        pub.foreach( k => k.export onSuccess {
          case exported => println(s"exported pub is ${exported}")
        })
        pub
      }
    } yield (rsa.verify(localPublicKey, signature.signedString, signedString)))
      .getOrElse(Future {
        println(s"no nie bylo klucza dla ${authorId}")
        println(s"za to byly dla ${this.publicKeys.keySet}")
        false
      })

  }

  def signAs(author: SigningId, message: String): Future[MessageSignature] = {
    println(s"@@called sign as for ${author.authorId}")
    val privateKey = this.privateKeys(author)
    val certificate = this.certificates(author)
    for {
      signature <- {
        println(s"final sign...........................${privateKey}")
        this.rsa.sign(privateKey, message)
      }
    } yield ({
      println("and it did it.... kurde")
      MessageSignature(signature, certificate.info)
    })
  }

  private def importKeys(privateKey: String, publicKey: String)
  : Future[RSAKeyPair] = {
    println("dooooing importKeys")
    val result = rsa.importPublic(publicKey).flatMap(importedPublicKey => {
      println(s"imported  public key ${importedPublicKey}")
      rsa.importPrivate(privateKey).map(importedPrivateKey => {
        println(s"imported  private key ${importedPrivateKey}")
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

