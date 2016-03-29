package pl.setblack.lsa.security

case class SigningId(authorId : String)

//name and key
case class CertificateInfo(publKeyHash : String, author : SigningId) {
  override def toString: String = {
    s"${publKeyHash}:${author.authorId}"
  }
}

//signed message sample
case class MessageSignature(signedString : String, signedBy : CertificateInfo)

//certificate signed by trusted
case class SignedCertificate(info : CertificateInfo,  signature: MessageSignature)

