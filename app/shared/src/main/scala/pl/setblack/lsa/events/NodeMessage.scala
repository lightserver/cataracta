package pl.setblack.lsa.events

/**
 *  Message is only known by Nodes.
 *
 */



case class NodeMessage(
                  val destination: Address,
                  val event: Event,
                  val route : Seq[Long] = Seq[Long]()) {

  def toTransport = NodeMessageTransport(destination.toTransport, Event.toExportedString(event), route)

}

case class NodeMessageTransport(val destination: AddressTransport, val event:String, route:Seq[Long] ) {
    def toNodeMessage  = NodeMessage ( destination.toAddress,  Event.fromExportedString(event)  , route)

    def toExportString  = upickle.default.write[NodeMessageTransport](this)
}

object NodeMessageTransport {
   def readExportedString(str: String) = upickle.default.read[NodeMessageTransport](str)
}