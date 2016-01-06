package pl.setblack.lsa.events

abstract class EventContext {
   def reply ( eventContent : String, path : Seq[String]) : Unit

  def sendLocal ( eventContent : String, path : Seq[String]) : Unit

  def isSecure () : Boolean
}

class NodeEventContext(private val parentNode : Node, private val sender: Long) extends EventContext{
   override def reply( eventContent : String, path : Seq[String]) : Unit = {
      val adr = Address(Target(sender), path)
        parentNode.sendEvent(  eventContent, adr, true)
    }

  override def sendLocal( eventContent : String, path : Seq[String]) : Unit = {
    val adr = Address(Local, path)
    parentNode.sendEvent(  eventContent, adr, true)
  }

  override def isSecure() : Boolean = {
    println("checking secure of ")
    parentNode.id.value.get.get == sender
  }

}

class NullContext extends EventContext{
  override def reply( eventContent : String, path : Seq[String]) : Unit = {
    println("I will do nothing")
  }

  override def sendLocal( eventContent : String, path : Seq[String]) : Unit = {
    println("I will do nothing")
  }

  override def isSecure() : Boolean = true
}
