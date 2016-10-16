package pl.setblack.lsa.events

import scala.concurrent.{ExecutionContext, Future, Promise}


class EventSequencer( implicit val executionContext : ExecutionContext) {
  private var lastNumberInSeq = 0L
  private var blocked = 0
  private var eventsPromise : Option[Promise[Long]] = None


  private def ensurePromise  = {
      eventsPromise = eventsPromise.orElse(Some( Promise[Long]))
  }

  def block(): Long = {
    blocked = blocked + 1
    ensurePromise
    println("blocked")
    lastNumberInSeq
  }

  def deblockAt(minEventId: Long): Long = {
    lastNumberInSeq = Math.max(lastNumberInSeq, minEventId)
    blocked = blocked - 1
    if ( blocked <= 0) {
      this.eventsPromise.map ( promise => promise.success(nextId()))
      this.eventsPromise = None
    }
    lastNumberInSeq
  }



  def nextEventId: Future[Long] = {
    println(s"have ${eventsPromise}")
    eventsPromise.fold(Future {nextId})( _.future)

  }

  private def nextId() = {
    lastNumberInSeq = lastNumberInSeq + 1
    lastNumberInSeq
  }
}


