package pl.setblack.lsa.events

/**
 *  Message is only known by Nodes.
 *
 */



case class NodeMessage(
                  val destination: Address,
                  val event: Event,
                  val route : Seq[Long] = Seq[Long]()) {

  def toTransport = NodeMessageTransport(destination.toTransport, event, route)

}

case class NodeMessageTransport(val destination: AddressTransport, val event:Event, route:Seq[Long] ) {
    def toNodeMessage  = NodeMessage ( destination.toAddress,  event  , route)
}
