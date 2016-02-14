package pl.setblack.lsa.events

class ProtocolBase extends Protocol{
  val connectionData  = new ConnectionData()
  override def send(msg: NodeMessage): Unit = {

   sendInternal(msg, connectionData)

  }

  def sendInternal (msg:NodeMessage, data :ConnectionData):Unit = ???


}
