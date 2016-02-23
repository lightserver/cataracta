package pl.setblack.lsa.events

import scala.collection.mutable.ArrayBuffer

class TextsDomain extends Domain[ArrayBuffer[String]](new ArrayBuffer[String](), Seq("")){
   override def processDomain( stateObject: ArrayBuffer[String], event : String, context :EventContext) = {
       stateObject += event

      DefaultResponse
   }

  override type EVENT = String



  override protected def getEventConverter: EventConverter[EVENT] = new EventConverter[String] {
    override def readEvent(str: String): String = str

    override def writeEvent(e: String): String = e
  }
}
