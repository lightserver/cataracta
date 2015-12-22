package pl.setblack.lsa.events

/**
 * Event with some content.
 *
 * Events  go to Domains.
 */
case class Event(
                val content: String,
                val id: Long,
                val sender: Long) {
}



