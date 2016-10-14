package pl.setblack.lsa.events

import scala.collection.mutable.ArrayBuffer

class TextsDomain(buffer : ArrayBuffer[String] = new ArrayBuffer) extends Domain[ArrayBuffer[String], String](
    buffer
     ) (TextDomain.stringConverter) {
   override def processDomain( stateObject: ArrayBuffer[String], event : String, context :EventContext) = {
       stateObject += event
      println(s" processed event ${event} and have ${stateObject}")
      new DefaultResponse
   }

}

object TextDomain {
  implicit  val stringConverter =  new EventConverter[String] {
    override def readEvent(str: String): String = str

    override def writeEvent(e: String): String = e
  }
}