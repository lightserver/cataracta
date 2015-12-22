package pl.setblack.mb137.data

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

class BoardMutable(val subject:String) {
  private  val messages = ArrayBuffer[BoardMessage]()

  val deleted  : mutable.Set[String] = mutable.Set()

  def append( msg : BoardMessage ) = {
    this.messages += msg
  }

  def delete ( uuid : String) = {
    this.deleted += uuid
    println(s" in deleted : ${uuid}")
  }


  def getDisplayedMessages( ):Seq[BoardMessage] = {
    this.messages.filter( m => !deleted.contains( m.uuid)).toSeq
  }
}
