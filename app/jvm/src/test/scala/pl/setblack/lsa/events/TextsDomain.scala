package pl.setblack.lsa.events

import scala.collection.mutable.ArrayBuffer

class TextsDomain extends Domain[ArrayBuffer[String]](new ArrayBuffer[String](), Seq("")){
   def processDomain( stateObject: ArrayBuffer[String], event : Event) = {
       event.content match  {
         case x:String => stateObject += x

       }
   }
 }
