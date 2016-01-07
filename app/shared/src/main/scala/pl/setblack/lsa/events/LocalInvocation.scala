package pl.setblack.lsa.events

class LocalInvocation(val target:Node) extends Protocol {
  val connectionData = new ConnectionData()
  override def send(msg: NodeMessage): Unit = {
      target.receiveMessage(msg,connectionData)
  }
}
