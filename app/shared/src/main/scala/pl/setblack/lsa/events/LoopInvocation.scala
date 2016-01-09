package pl.setblack.lsa.events

class LoopInvocation(val target:Node) extends Protocol {
 val connectionData  = new ConnectionData()


  override def send(msg: NodeMessage): Unit = {

    target.receiveMessageLocal(msg, connectionData)
  }
}
