package pl.setblack.lsa.events

import scala.collection.mutable

class ConnectionData {
  val connectionData = mutable.Map[String, Any]()
  val domains = mutable.Set[Seq[String]]()

   def getConnectionObject(key : String) : Option[Any] = {
     connectionData.get(key)
   }

  def setConnectionObject(key : String, value : Any) = {
    this.connectionData.put(key, value)
  }

  def trackDomain(path : Seq[String]) = {
    domains += path
  }

  def isDomainTracked( path: Seq[String]) = {
    //TODO possible repetition/change of Node code
    domains.contains (path)
  }

}
