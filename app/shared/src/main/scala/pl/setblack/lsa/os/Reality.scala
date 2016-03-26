package pl.setblack.lsa.os

import pl.setblack.lsa.concurrency.ConcurrencySystem
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.security.SecurityProvider

trait Reality {
  def storage: Storage

  def concurrency: ConcurrencySystem

  def security: SecurityProvider
}


case class SimpleReality(
                          override val storage: Storage,
                          override val concurrency: ConcurrencySystem,
                          override val security: SecurityProvider
                        ) extends Reality

