package pl.setblack.mb137.data

import pl.setblack.lsa.events.{Event, Domain}
import pl.setblack.lsa.io.Storage

import upickle.default._

class BoardDomain( topic :String, path: Seq[String]) extends  Domain[BoardMutable](new BoardMutable("default"), path){
  override def processDomain(state: BoardMutable, event: Event): Unit = {
      println("processing board event")
      val boardEv = BoardEvent.readBoardEvent(event.content)
      println("event is " + boardEv.toString)
      boardEv match {
        case NewPost(msg, author, timestamp, uuid) =>
          state.append( BoardMessage(author, msg, timestamp,uuid))
        case DeletePost(uuid) =>
          state.delete(uuid)
      }
    println("listeners:" + listeners.toString)
  }
}
