package pl.setblack.lsa.events

/**
 * Used to send messages between Nodes.
 */
trait Protocol {
    def send ( msg:NodeMessage): Unit


}


