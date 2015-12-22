package pl.setblack.lsa.events

/**
 * Created by jarek on 9/15/15.
 */
trait MessageListener {
    def onMessage( m : NodeMessage):Unit = ???
}
