package pl.setblack.lsa.events

/**
 * Event with some content.
 *
 * Events  go to Domains.
 */
case class Event(
                content: String,
                id: Long,
                sender: Long,
                transient :Boolean = false) {
}



