package pl.setblack.lsa.events


sealed trait Response[T] {
  def persist: Boolean

  def register: Boolean

  def alreadyProcessed: Boolean

  def newState: Option[T]
}


case class DefaultResponse[T](override val newState: Option[T] = None) extends Response[T] {
  override def persist: Boolean = true

  override def register: Boolean = true

  override def alreadyProcessed: Boolean = false
}

case class PreviouslySeenEvent[T](override val newState: Option[T] = None) extends Response[T] {
  override def persist: Boolean = false

  override def register: Boolean = false

  override def alreadyProcessed: Boolean = true
}

case class TransientEvent[T](override val newState: Option[T] = None) extends Response[T] {
  override def persist: Boolean = false

  override def register: Boolean = false

  override def alreadyProcessed: Boolean = false
}

case object  Invalid extends Response[Nothing] {

  override def persist: Boolean = false

  override def register: Boolean = false

  override def alreadyProcessed: Boolean = false

  override def newState = None
}