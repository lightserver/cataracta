package pl.setblack.lsa.events

import scala.Predef
import scala.collection.mutable.Set

class NodeConnection(
                      val targetId: Long,
                      val protocol: Protocol,
                      val listeningTo: Set[Seq[String]] = Set(Seq())) {
  def setListeningTo(domains: Predef.Set[Seq[String]]): Unit = {
    listeningTo ++= domains
  }

  def send(msg: NodeMessage): Unit = {
    if ( msg.destination.target == System || isInterested( msg.destination.path) ) {
      protocol.send(msg)
    } else {
      println(s"ignored sending to ${msg.destination}")
    }
  }

  private def isInterested( path : Seq[String]): Boolean = {
    if ( listeningTo.contains(path) ) {
      return true
    } else {
      path match {
        case seq :+ last => return isInterested(seq)
        case _ => return false
      }
    }
  }

  def knows(id: Long): Boolean = {
    id == targetId
  }
}


