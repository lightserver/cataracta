package pl.setblack.lsa.events

import scala.collection.mutable

class ConnectionData {
  val connectionData = mutable.Map[String, Any]()

   def getConnectionObject(key : String) : Option[Any] = {
     connectionData.get(key)
   }

  def setConnectionObject(key : String, value : Any) = {
    this.connectionData.put(key, value)
  }



}
