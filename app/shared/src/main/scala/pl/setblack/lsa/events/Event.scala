package pl.setblack.lsa.events

import pl.setblack.lsa.security.MessageSignature

/**
 * Event with some content.
 *
 * Events  go to Domains.
 */

sealed trait Event {
  def content :String
  def id : Long
  def sender : Long
  def isSigned : Boolean
}


case class UnsignedEvent(
                content: String,
                id: Long,
                sender: Long
                ) extends Event{
  override def isSigned: Boolean = false
}

case class SignedEvent(
                          content: String,
                          id: Long,
                          sender: Long,
                          signature : MessageSignature

                        ) extends Event{
  override def isSigned: Boolean = true
}

object Event {
  def toExportedString(ev :Event) : String = upickle.default.write(ev)
  def fromExportedString(str :String) : Event = upickle.default.read[Event](str)
}
