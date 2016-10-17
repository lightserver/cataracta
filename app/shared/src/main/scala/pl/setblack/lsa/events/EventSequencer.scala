package pl.setblack.lsa.events

import scala.concurrent.{ExecutionContext, Future, Promise}


class EventSequencer( implicit val executionContext : ExecutionContext) {
  private var lastNumberInSeq = 0L
  private var blocked = 0
  private var eventsPromise : List[Promise[Long]] = Nil


  def block(): Long = {
    blocked = blocked + 1
    println("blocked")
    lastNumberInSeq
  }

  def deblockAt(minEventId: Long): Long = {
    lastNumberInSeq = Math.max(lastNumberInSeq, minEventId)
    blocked = blocked - 1
    if ( blocked <= 0) {
      this.eventsPromise.foreach( promise => promise.success(nextId()))
      this.eventsPromise = Nil
    }
    lastNumberInSeq
  }

  def nextEventId: Future[Long] = {
    if ( blocked > 0 ) {
      val  promise = Promise[Long]
      eventsPromise = eventsPromise :+ promise
      promise.future
    } else {
      Future{nextId}
    }
  }

  private def nextId() = {
    lastNumberInSeq = lastNumberInSeq + 1
    lastNumberInSeq
  }
}


