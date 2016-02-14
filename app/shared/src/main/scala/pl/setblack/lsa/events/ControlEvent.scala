package pl.setblack.lsa.events


import upickle.default._
sealed trait ControlEvent {
}

case class RegisteredClient(clientId :Long, senderNodeId:Long) extends ControlEvent
case class ResyncDomain(  clientId:Long, domain: Seq[String],  recentEvents : Map[Long, Long],  syncBack: Boolean ) extends ControlEvent
case class RestoreDomain( domain: Seq[String],  serialized: String ) extends ControlEvent
case class ListenDomains( domains: Set[Seq[String]]) extends ControlEvent




object ControlEvent {
  def parseControlEvent(ev : String ): ControlEvent =  {
      read[ControlEvent](ev)
  }

  def writeEvent(ev :ControlEvent) : String = {
    val result = write(ev)
    result
  }
}