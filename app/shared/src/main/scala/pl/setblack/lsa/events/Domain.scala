package pl.setblack.lsa.events

abstract class Domain[O](private var domainState: O, val path: Seq[String]) {

  type EVENT

  var recentEvents:scala.collection.mutable.Map[Long,Seq[Long]] = scala.collection.mutable.HashMap[Long, Seq[Long]]()
  var listeners = Seq[DomainListener[O, EVENT]]()
  val eventsHistory = scala.collection.mutable.ArrayBuffer.empty[Event]

  private[events] def getSerializer : Option[DomainSerializer[O]] = None

  def getEventConverter : EventConverter[EVENT]

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
    //OMG how naive
    this.eventsHistory.filter( ev=> recentEvents.get(ev.sender).map( ev.id > _ ).getOrElse(true) )
  }

  def receiveEvent(event: Event, eventContext :EventContext):Response = {
    if (!seenEvent(event)) {


      recentEvents = recentEvents + (event.sender -> (
        recentEvents.getOrElse(event.sender, Seq()) :+ event.id))
      val convertedEvent = getEventConverter.readEvent(event.content)
      val result = processDomain(domainState, convertedEvent, eventContext)
      if ( result.persist) {
          eventsHistory += event
      }
      listeners.foreach(l => l.onDomainChanged(domainState, Some(convertedEvent)))
      result
    } else {
      PreviouslySeenEvent
    }
  }

  def registerListener(listener: DomainListener[O, EVENT]): Unit = {
    listeners = listeners :+ listener
  }


}
