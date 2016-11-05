package pl.setblack.lsa.events

import slogging.{LazyLogging, LoggerFactory}

abstract class Domain[O, EVENT](private var domainState: O)
                               (implicit val eventConverter: EventConverter[EVENT])
  extends LazyLogging {

  var recentEvents: scala.collection.mutable.Map[Long, Seq[Long]] = scala.collection.mutable.HashMap[Long, Seq[Long]]()
  var listeners = Seq[DomainListener[O, EVENT]]()
  val eventsHistory = scala.collection.mutable.ArrayBuffer.empty[Event]

  private[events] def getSerializer: Option[DomainSerializer[O]] = None

  protected def processDomain(state: O, event: EVENT, eventContext: EventContext): Response[O]

  private def processDomainInternal(state: O, event: EVENT, eventContext: EventContext): Response[O] = {
    val response = processDomain(state, event, eventContext)
    response.newState.foreach(setState(_))
    response
  }

  def getState = domainState

  private[events] def setState(newState: O) = {
    domainState = newState
    //listeners.foreach(l => l.onDomainChanged(domainState, None))
  }

  private def seenEvent(event: Event): Boolean = {
    recentEvents.getOrElse(event.sender, Seq()).contains(event.id)
  }

  def eventsToResend(clientId: Long, recentEvents: Map[Long, Long]): Seq[Event] = {
    //OMG how naive
    this.eventsHistory.filter(ev => recentEvents.get(ev.sender).map(ev.id > _).getOrElse(true))
  }

  def receiveEvent(event: Event, eventContext: EventContext): Response[O] = {
    if (!seenEvent(event)) {
      logger.debug(s"NEW[${event.id}]")
      recentEvents = recentEvents + (event.sender -> (
        recentEvents.getOrElse(event.sender, Seq()) :+ event.id))
      val convertedEvent = eventConverter.readEvent(event.content)
      val result = processDomainInternal(domainState, convertedEvent, eventContext)
      if (result.persist) {
        eventsHistory += event
      }
      listeners.foreach(l => l.onDomainChanged(domainState, Some(convertedEvent)))
      result
    } else {
      logger.debug(s"SEEN[${event.id}]")
      new PreviouslySeenEvent[O]
    }
  }

  def registerListener(listener: DomainListener[O, EVENT]): Unit = {
    listeners = listeners :+ listener
    //@TODO: introduce system events on listeners (like DomainInitialized etc., DomainLoaded
    listener.onDomainChanged(domainState, None)
  }


}
