package pl.setblack.lsa.events

import java.util.UUID
import java.util.concurrent.locks.{Lock, ReentrantLock}

import pl.setblack.lsa.io.Storage

class HashStorage extends Storage {
  var storedValues = Map[Seq[String], String]()
  var lockAfter: Option[Int] = None
  private val lock = new ReentrantLock()

  override def save(value: String, path: Seq[String]): Unit = {
    maybeLock{() => storedValues = storedValues + (path -> value)}
  }

  override def load(path: Seq[String]): Option[String] = {
    maybeLock{() => {
      var  result = storedValues.get(path)

      result
    }}
  }

  private def maybeLock[T](body: () => T):T= {
    val myLock = synchronized{lockAfter = lockAfter.map(v => v - 1); lockAfter}.filter( _ < 0)
    myLock.foreach{_ =>lock.lock()}
    val result = body()
    myLock.foreach{_ =>lock.unlock()}
    result
  }

  def blockAfter(ops: Int): Lock = {
    synchronized(this.lockAfter = Some(ops))
    lock.lock()
    lock
  }

  def unlock() : Lock = {
    synchronized(this.lockAfter = None)
    lock.unlock()
    lock
  }

}
