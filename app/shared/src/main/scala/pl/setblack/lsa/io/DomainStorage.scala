package pl.setblack.lsa.io

import pl.setblack.lsa.concurrency.BadActorRef
import pl.setblack.lsa.events.impl.{EventWrapper, SendEventCommand}
import pl.setblack.lsa.events.{Domain, Event, EventContext}
import pl.setblack.lsa.io.DataStorage.{DataInputStream, DataStorage, NoMoreData}
import slogging.StrictLogging
import upickle.default._

import scala.concurrent.{ExecutionContext, Future, Promise}

class DomainStorage(val path: Seq[String], val sysStorage: DataStorage)(implicit val ec: ExecutionContext) extends StrictLogging {
  var saveCounter: Int = 0


  lazy val storFuture = {
    sysStorage.openDataWriter(path)
  }


  def saveEvent(event: Event): Unit = {
    storFuture.onSuccess { case writer => writer.writeNextValue(write[Event](event)) }
  }

  def saveDomain(domain: Domain[_, _]) = {
  }


  private def processLoadingEvents(
                                    domainActorRef: BadActorRef[EventWrapper],
                                    ctx: EventContext,
                                    inputStorage: DataInputStream,
                                    nextIdPromise: Promise[Long],
                                    maxId: Long
                                  ) {
    val nextValFuture = inputStorage.readNextValue()
    nextValFuture.onSuccess {
      case Right(value) => {
        val event = read[Event](value)
        val newMax = Math.max(maxId, event.id)
        logger.info(s"sending(event.id): ${event} to ${path} ctx:${ctx.getClass}")
        domainActorRef.send(SendEventCommand(event, ctx))
        processLoadingEvents(domainActorRef, ctx, inputStorage, nextIdPromise, newMax)
      }
      case Left(error) => {
        error match {
          case NoMoreData => nextIdPromise.success(maxId)
          case _ => {
            logger.error(s"load error ${error}")
          }
        }
      }
    }
  }

  def loadEvents(domain: Domain[_, _], ctx: EventContext, domainActor: BadActorRef[EventWrapper]): Future[Long] = {
    val nextIdPromise = Promise[Long]
    val reader = sysStorage.openDataReader(path)
    reader.onSuccess {
      case Some(file) => processLoadingEvents(domainActor, ctx, file, nextIdPromise, 0)
      case None => nextIdPromise.success(0)
    }
    nextIdPromise.future
  }


}


