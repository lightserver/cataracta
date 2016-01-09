package pl.setblack.lsa.events


import upickle.default._
sealed trait ControlEvent {
}

case class RegisteredClient(val clientId :Long, val senderNodeId:Long) extends ControlEvent
case class ResyncDomain( val clientId:Long,val domain: Seq[String], val recentEvents : Map[Long, Seq[Long]], val syncBack: Boolean ) extends ControlEvent



object ControlEvent {
  def parseControlEvent(ev : String ): ControlEvent =  {
      read[ControlEvent](ev)
  }

  def writeEvent(ev :ControlEvent) : String = {

    write[ControlEvent](ev)
  }
}