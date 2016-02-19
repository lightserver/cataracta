package pl.setblack.lsa.events

trait EventConverter[E] {
  def readEvent(str: String): E

  def writeEvent(e: E): String
}
