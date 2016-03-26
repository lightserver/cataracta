package pl.setblack.lsa.security

//name and key
case class CertificateInfo(publKeyHash : String, author : String)

//signed message sample
case class MessageSignature(signedString : String, signedBy : CertificateInfo)

//certificate signed by trusted
case class SignedCertificate(info : CertificateInfo,  signature: MessageSignature)

