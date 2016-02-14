package pl.setblack.lsa.events

class LoopInvocation(val target:Node) extends ProtocolBase {

  override def sendInternal(msg: NodeMessage, connectionData: ConnectionData): Unit = {
    target.receiveMessageLocal(msg, connectionData)
  }
}
