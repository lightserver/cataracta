package pl.setblack.lsa.events

trait DomainListener[O] {

  def onDomainChanged ( domain: O , ev : Option[Event]) : Unit
}
