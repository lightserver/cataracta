package pl.setblack.lsa.security

class SecurityProvider {
  type SigningID = String

  //there may be more root CA
  def registerRootCA(ca : SigningID, exportedKey : RSAKeyPairExported ) = ???

  def registerSigner(signer : SigningID, exportedKey : RSAKeyPairExported ) = ???

  def registerCertificate(author: SigningID, signedCert  :SignedCertificate) = ???

  def signCertificate(signer : SigningID, cert : CertificateInfo ) : SignedCertificate = ???

  def signAs(author : SigningID, message : String) : MessageSignature = ???


}


case class RSAKeyPairExported( publicKey  :String, privateKey : String)