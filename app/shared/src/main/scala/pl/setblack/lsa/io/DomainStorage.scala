package pl.setblack.lsa.io

import pl.setblack.lsa.events.{Domain, Event, EventContext, NullContext}
import slogging.StrictLogging
import upickle.default._

class DomainStorage(val path: Seq[String], val sysStorage: Storage) extends StrictLogging {
  var saveCounter: Int = 0

  def saveEvent(event: Event): Unit = {
    val storePath = getStorePath(nextCounter)

    sysStorage.save(write[Event](event), storePath)

    sysStorage.save(saveCounter.toString, getSummaryPath())
  }

  def saveDomain(domain: Domain[_, _]) = {


  }

  private def loadEvent(number: Integer): Option[Event] = {
    val storePath = getStorePath(number)
    sysStorage.load(storePath) match {
      case Some(s) => {
        try {
          Some(read[Event](s))
        } catch {
          case e: Exception => {
            logger.error(s"error parsing JSON: \n${s}", e)
              throw new RuntimeException(e)
          }
        }
      }
      case _ => None
    }
  }

  def loadEvents(domain: Domain[_, _], ctx : EventContext): Long = {

    sysStorage.load(getSummaryPath()).map(
      storedNumber => {
        val maxEvent = storedNumber.toInt
        for (i <- 1 to maxEvent) {
          loadEvent(i).foreach(e => domain.receiveEvent(e, ctx))
        }
        saveCounter = maxEvent
        saveCounter
      }
    ).getOrElse(0).toLong
  }

  private def getStorePath(cnt: Integer) = {
    Seq("events") ++ path :+ cnt.toString
  }

  private def getSummaryPath() = {
    Seq("events") ++ path :+ "summary"
  }

  private def nextCounter = {
    saveCounter = saveCounter + 1
    saveCounter
  }
}


