package pl.setblack.lsa.events

class LocalInvocation(val target:Node) extends ProtocolBase {
  override def sendInternal(msg: NodeMessage, connectionData: ConnectionData): Unit = {
      target.receiveMessage(msg,connectionData)
  }
}
