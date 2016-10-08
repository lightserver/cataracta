package pl.setblack.lsa.events

import scala.collection.mutable.ArrayBuffer

class TextsDomain extends Domain[ArrayBuffer[String], String](
    new ArrayBuffer[String]()
     ) (TextDomain.stringConverter)
      {
   override def processDomain( stateObject: ArrayBuffer[String], event : String, context :EventContext) = {
       stateObject += event

      DefaultResponse
   }

}

object TextDomain {
  implicit  val stringConverter =  new EventConverter[String] {
    override def readEvent(str: String): String = str

    override def writeEvent(e: String): String = e
  }
}