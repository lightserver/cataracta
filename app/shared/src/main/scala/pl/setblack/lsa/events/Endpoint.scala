package pl.setblack.lsa.events


sealed trait Endpoint {
 def toLong:Long = 0


}

case object Local extends Endpoint {
  override def toLong:Long = -1
}

case object All extends Endpoint {
  override def toLong:Long = -2
}

case object System extends Endpoint {
  override def toLong:Long = -3
}

case class Target(val id:Long) extends Endpoint {
  override def toLong:Long = id
}

object Endpoint {
  def toEndpoint (adr : Long ) : Endpoint = adr match {
    case -1 => Local
    case -2 => All
    case -3 => System
    case x => Target(x)
  }
}


