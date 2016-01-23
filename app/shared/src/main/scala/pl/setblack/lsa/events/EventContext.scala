package pl.setblack.lsa.events

abstract class EventContext {
   def reply ( eventContent : String, path : Seq[String]) : Unit

  def sendLocal ( eventContent : String, path : Seq[String]) : Unit = {
    val adr = Address(Local, path)
    send(adr, eventContent)
  }

  def send (address: Address,  eventContent : String) : Unit

  def isSecure () : Boolean

  def connectionData : ConnectionData
}

class NodeEventContext(
                        private val parentNode : Node,
                        private val sender: Long,
                      val connectionData: ConnectionData) extends EventContext{
   override def reply( eventContent : String, path : Seq[String]) : Unit = {
      val adr = Address(Target(sender), path)
        parentNode.sendEvent(  eventContent, adr, true)
    }

  override def send( adr: Address, eventContent : String) : Unit = {
    parentNode.sendEvent(  eventContent, adr, false)
  }

  override def isSecure() : Boolean = {

    parentNode.id.value.get.get == sender
  }

}

class NullContext extends EventContext{

  override val connectionData = new ConnectionData()

  override def reply( eventContent : String, path : Seq[String]) : Unit = {

  }

  override def send(address: Address, eventContent: String): Unit = {

  }
  override def isSecure() : Boolean = true


}
