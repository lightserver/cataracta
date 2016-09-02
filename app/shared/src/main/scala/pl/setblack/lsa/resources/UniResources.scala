package pl.setblack.lsa.resources

import scala.concurrent.Future
import scala.util.Try

trait UniResources {
  def getResource( path : String) : Future[Try[UniResource]]
}
