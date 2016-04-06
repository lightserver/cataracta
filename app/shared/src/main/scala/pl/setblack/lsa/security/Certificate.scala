package pl.setblack.lsa.security

import java.time.{Instant, LocalDateTime}

case class SigningId(authorId: String)

//name and key
case class CertificateInfo(
                            publKey: String,
                            author: SigningId,
                            privileges: Set[String],
                            validTo: String ) {
  override def toString: String = {
    s"${publKey}@${author.authorId}:${privileges}<${validTo.toString}"
  }
}
//signed message sample
case class MessageSignature(signedString: String, signedBy: SignedCertificate)

//certificate signed by trusted
case class SignedCertificate(info: CertificateInfo, signature: CertificateSignature)

case class CertificateSignature(signedString: String, signedBy: CertificateInfo)
