package pl.setblack.lsa.events

import slogging.LazyLogging

import scala.Predef


class NodeConnection(
                      val targetId: Long,
                      val protocol: Protocol,
                      var listeningTo: Set[Seq[String]] = Set(Seq()))  extends LazyLogging{
  def setListeningTo(domains: Predef.Set[Seq[String]]): Unit = {
    listeningTo = domains
  }

  def send(msg: NodeMessage): Unit = {
    logger.debug(s"sending to ${targetId}")
    if ( msg.destination.target == System || isInterested( msg.destination.path) ) {
      println(s"${targetId} wants  ${msg.destination.path}  event ${msg.event.content.take(50)} seen by ${msg.route}")
      if ( msg.route.dropRight(1).contains(targetId)) {
          println("futile send of probably already seen message  -but jarek is not sure...")
      }
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


