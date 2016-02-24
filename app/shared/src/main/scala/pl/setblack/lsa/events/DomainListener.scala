package pl.setblack.lsa.events

trait DomainListener[O, EVENT] {

  def onDomainChanged ( domain: O , ev : Option[EVENT]) : Unit
}
