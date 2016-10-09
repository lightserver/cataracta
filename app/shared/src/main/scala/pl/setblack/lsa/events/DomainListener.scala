package pl.setblack.lsa.events

trait DomainListener[O, EVENT] {

  def onDomainChanged ( domainState: O , ev : Option[EVENT]) : Unit
}
