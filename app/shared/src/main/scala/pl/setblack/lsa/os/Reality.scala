package pl.setblack.lsa.os

import pl.setblack.lsa.concurrency.ConcurrencySystem
import pl.setblack.lsa.io.DataStorage.DataStorage
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.resources.UniResources
import pl.setblack.lsa.security.SecurityProvider

import scala.concurrent.Future

trait Reality {
  def storage: DataStorage

  def concurrency: ConcurrencySystem

  def security: Future[SecurityProvider]

  def resources: UniResources
}


case class SimpleReality(
                          override val storage: DataStorage,
                          override val concurrency: ConcurrencySystem,
                          override val security: Future[SecurityProvider],
                          override val resources : UniResources
                        ) extends Reality

