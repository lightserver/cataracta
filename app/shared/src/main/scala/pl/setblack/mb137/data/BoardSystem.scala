package pl.setblack.mb137.data

import java.util.Date

import pl.setblack.lsa.events.Node
import pl.setblack.lsa.io.Storage
import upickle._


abstract class BoardSystem {
  val  storage = createStorage()
  val mainNode = createMainNode()

  mainNode.registerDomain(Seq("default"), new BoardDomain("default", Seq("default")))

  load()
  resync()

   def createMainNode():Node

   def createStorage() : Storage


  def enterMessage( txt: String, author:String) = {
    val uuid = java.util.UUID.randomUUID.toString
    val newPost = NewPost(txt, author, new Date().getTime(), uuid)

    mainNode.sendEvent(BoardEvent.writeBoardEvent(newPost) ,Seq("default"))
  }

  def deleteMessage( uuid: String) = {
   val deletePost = DeletePost( uuid)


    mainNode.sendEvent(BoardEvent.writeBoardEvent(deletePost) ,Seq("default"))
  }

  def getBoardMutable():BoardMutable  = {
    mainNode.getDomainObject(Seq("default")).asInstanceOf[BoardMutable]
  }

  def save() = {
    //this.mainNode.saveDomains(storage)
  }

  def load() = {
    this.mainNode.loadDomains()
  }

  def resync() = {
    println("resync...")
    this.mainNode.resync()
  }
}



