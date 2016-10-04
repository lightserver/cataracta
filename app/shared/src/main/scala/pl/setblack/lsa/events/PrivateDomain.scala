package pl.setblack.lsa.events

import pl.setblack.lsa.secureDomain.SecurityEvent



abstract class PrivateDomain[O](domainState: O, path: Seq[String])
  extends Domain[O, SecurityEvent](domainState, path) {
  override def eventsToResend(clientId: Long, recentEvents: Map[Long, Long]): Seq[Event] = {
    Seq()
  }
}
