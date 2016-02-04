package pl.setblack.lsa.events


import upickle.default._
sealed trait ControlEvent {
}

case class RegisteredClient(val clientId :Long, val senderNodeId:Long) extends ControlEvent
case class ResyncDomain( val clientId:Long,val domain: Seq[String], val recentEvents : Map[Long, Long], val syncBack: Boolean ) extends ControlEvent
case class RestoreDomain( val domain: Seq[String], val serialized: String ) extends ControlEvent



object ControlEvent {
  def parseControlEvent(ev : String ): ControlEvent =  {
    println(s"reading <<< ${ev}")
      read[ControlEvent](ev)
  }

  def writeEvent(ev :ControlEvent) : String = {
    println(s"writing >>> ${ev}")
    val result = write(ev)
    println(s"written > ${result}")
    result
  }
}