package pl.setblack.lsa.events

class LoopInvocation(val target:Node) extends Protocol {

  override def send(msg: NodeMessage): Unit = {
    println("passing message through local loop")
    target.receiveMessageLocal(msg)
  }
}
