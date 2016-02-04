package pl.setblack.lsa.events


sealed trait Response {
   def persist : Boolean
   def register : Boolean
   def alreadyProcessed : Boolean

}
case object DefaultResponse extends Response{
  override def persist: Boolean = true
  override def register: Boolean = true
  override  def alreadyProcessed : Boolean =  false
}

case object PreviouslySeenEvent extends Response {
  override def persist: Boolean = false
  override def register: Boolean = false
  override  def alreadyProcessed : Boolean =  true
}
case object TransientEvent extends Response {
  override def persist: Boolean = false
  override def register: Boolean = false
  override  def alreadyProcessed : Boolean =  false
}

case object Invalid extends Response {
  override def persist: Boolean = false
  override def register: Boolean = false
  override  def alreadyProcessed : Boolean =  false
}