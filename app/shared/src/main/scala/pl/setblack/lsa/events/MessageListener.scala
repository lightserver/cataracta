package pl.setblack.lsa.events


trait MessageListener {
    def onMessage( m : NodeMessage):Unit = ???
}
