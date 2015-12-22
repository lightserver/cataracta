package pl.setblack.lsa.io

import pl.setblack.lsa.events.{Domain, Event}

import upickle.default._

class DomainStorage(val path: Seq[String], val sysStorage : Storage) {
  var saveCounter :Int= 0



  def saveEvent(event: Event):Unit = {
    val storePath = getStorePath( nextCounter )
    println("storing in "+storePath+" event:" + event )
    sysStorage.save( write[Event]( event) , storePath)

    sysStorage.save( saveCounter.toString, getSummaryPath())
  }

  def saveDomain( domain : Domain[_]) = {


  }

 private def loadEvent(number: Integer ) : Option[Event] = {
   val storePath = getStorePath( number )
   sysStorage.load(storePath) match {
     case Some(s)  => {
       println("loading event ..............." + number)
       Some(read[Event](s))
     }
     case   _ => None
   }
 }

  def loadEvents(domain: Domain[_]):Long = {
    sysStorage.load(getSummaryPath()).map (
      storedNumber => {
        val maxEvent = storedNumber.toInt
        for(  i  <- 0 to maxEvent ){
            loadEvent(i).foreach( e => domain.receiveEvent(e))
        }
        saveCounter = maxEvent
        saveCounter
      }
    ).getOrElse(0).toLong
  }

  private def getStorePath(cnt :Integer) = {
      Seq("events") ++ path :+ cnt.toString
  }

  private def getSummaryPath() = {
    Seq("events") ++ path :+ "summary"
  }

  private def nextCounter = {
    saveCounter = saveCounter +1
    saveCounter
  }
}


