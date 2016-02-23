package pl.setblack.lsa.events

abstract class Domain[O](private var domainState: O, val path: Seq[String]) {

  type EVENT

  var recentEvents = Map[Long, Seq[Long]]()
  var listeners = Seq[DomainListener[O]]()
  val eventsHistory = scala.collection.mutable.ArrayBuffer.empty[Event]

  private[events] def getSerializer : Option[DomainSerializer[O]] = None

  protected def getEventConverter : EventConverter[EVENT]

  protected def processDomain(state : O, event:EVENT, eventContext : EventContext ) : Response

  private def processDomain(state: O, event: Event, eventContext: EventContext ): Response = {
      processDomain(state, getEventConverter.readEvent(event.content), eventContext)
  }

  def getState = domainState

  def setState( newState : O ) = {
    domainState = newState
    listeners.foreach(l => l.onDomainChanged(domainState, None))
  }

  private def seenEvent(event: Event ) : Boolean = {
    recentEvents.getOrElse(event.sender, Seq()).contains( event.id)
  }

  def eventsToResend(clientId: Long, recentEvents: Map[Long, Long]): Seq[Event] = {
    this.eventsHistory
  }

  def receiveEvent(event: Event, eventContext :EventContext):Response = {
    if (!seenEvent(event)) {

      recentEvents = recentEvents + (event.sender -> (
        recentEvents.getOrElse(event.sender, Seq()) :+ event.id))

      val result = processDomain(domainState, event, eventContext)
      if ( result.persist) {
          eventsHistory += event
      }
      listeners.foreach(l => l.onDomainChanged(domainState, Some(event)))
      result
    } else {
      PreviouslySeenEvent
    }
  }

  def registerListener(listener: DomainListener[O]): Unit = {
    listeners = listeners :+ listener
  }


}
