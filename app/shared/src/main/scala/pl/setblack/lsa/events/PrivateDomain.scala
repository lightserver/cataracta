package pl.setblack.lsa.events

import pl.setblack.lsa.secureDomain.SecurityEvent



abstract class PrivateDomain[O](domainState: O)
  extends Domain[O, SecurityEvent](domainState) {
  override def eventsToResend(clientId: Long, recentEvents: Map[Long, Long]): Seq[Event] = {
    Seq()
  }
}
