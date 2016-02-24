package pl.setblack.lsa.events

class DomainRef[EVENT](val path:Seq[String],  val converter: EventConverter[EVENT], val node: Node) {

   def send(e: EVENT,  endpoint: Endpoint = All ): Unit = {
      node.sendEvent(converter.writeEvent(e),Address(endpoint, path),false)
   }

}
