package pl.setblack.lsa.events

abstract class PrivateDomain[O](domainState: O, path: Seq[String]) extends Domain[O](domainState, path) {
  override def eventsToResend(clientId: Long, recentEvents: Map[Long, Long]): Seq[Event] = {
    Seq()
  }
}
