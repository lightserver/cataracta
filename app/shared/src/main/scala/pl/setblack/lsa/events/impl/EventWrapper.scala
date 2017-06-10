package pl.setblack.lsa.events.impl

import pl.setblack.lsa.events._

/* Event below were devoted to Lord of Change. */
sealed trait EventWrapper




case object LoadDomainCommand extends EventWrapper

case class SendEventCommand(
                             event: Event,
                             ctx: EventContext) extends EventWrapper {
}

/** received sync */
case class ResyncDomainCommand(sync: ResyncDomain, nodeId: Long) extends EventWrapper

/** trigger sync */
case class SyncDomainCommand(nodeId: Long, syncBack: Boolean) extends EventWrapper

case class RestoreDomainCommand(sync: RestoreDomain) extends EventWrapper

case class RegisterListener[O, X](listener: DomainListener[O, X]) extends EventWrapper

case object EndLoading extends EventWrapper

