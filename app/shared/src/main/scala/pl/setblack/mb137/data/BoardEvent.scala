package pl.setblack.mb137.data

import upickle.default._

sealed trait  BoardEvent  {

}


case class NewPost(val message: String, val author: String, timestamp : Long,  uuid :String) extends BoardEvent {

}

case class DeletePost(val uuid: String) extends BoardEvent {

}

object BoardEvent {
   def  writeBoardEvent(b:BoardEvent) :String =  {
      write[BoardEvent](b)
   }

   def  readBoardEvent(b:String) :BoardEvent =  {
      read[BoardEvent](b)
   }
}
