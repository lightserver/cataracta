package pl.setblack.lsa.events

class LoopInvocation(val target:Node) extends Protocol {
 val connectionData  = new ConnectionData()


  override def send(msg: NodeMessage): Unit = {
    println("passing message through local loop")
    target.receiveMessageLocal(msg, connectionData)
  }
}
