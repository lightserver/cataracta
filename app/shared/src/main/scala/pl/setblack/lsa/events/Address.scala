package pl.setblack.lsa.events

case class Address(val target: Endpoint = All, val path:Seq[String]=Seq()) {

  def toTransport = AddressTransport(target.toLong, path)
}

case class AddressTransport( val target:Long, val path:Seq[String]) {
  def toAddress = {
    Address( Endpoint.toEndpoint(target), path)
  }
}


