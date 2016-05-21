package pl.setblack.lsa.events

class LoopInvocation(val target:Node) extends ProtocolBase {

  override def sendInternal(msg: NodeMessage, connectionData: ConnectionData): Unit = {
    val certificate = msg.event match {
      case signed : SignedEvent => Some(signed.signature.signedBy.info)
      case _ => None
    }
    val ctx = new NodeEventContext( target, msg.event.sender, connectionData, certificate )
    target.receiveMessageLocal(msg, ctx)
  }
}
