package pl.setblack.lsa.events

import java.util.concurrent.locks.{Lock, ReentrantLock}

import pl.setblack.lsa.io.DataStorage._

import scala.concurrent.{ExecutionContext, Future}

class HashStorage(implicit val executionContext: ExecutionContext) extends DataStorage {
  @volatile var storedValues = Map[Seq[String], List[String]]()
  var lockAfter: Option[Int] = None
  private val lock = new ReentrantLock()

  override def openDataReader(path: Seq[String]): Future[Option[DataInputStream]] = {
    Future {
      storedValues.get(path).map(l => new ListReader(l))
    }
  }

  override def openDataWriter(path: Seq[String]): Future[DataOutputStream] = {
    Future {
      new Writer(path)
    }
  }

  def save(path: Seq[String], value: String): Unit = {
    storedValues = storedValues + (path -> storedValues.get(path).orElse(Some(List[String]())).map(list => list :+ value).get)
  }

  private def maybeLock[T](body: () => T): T = {
    val myLock = synchronized {
      lockAfter = lockAfter.map(v => v - 1);
      lockAfter
    }.filter(_ < 0)
    myLock.foreach { _ => lock.lock() }
    val result = body()
    myLock.foreach { _ => lock.unlock() }
    result
  }

  def blockAfter(ops: Int): Lock = {
    synchronized(this.lockAfter = Some(ops))
    lock.lock()
    lock
  }

  def unlock(): Lock = {
    synchronized(this.lockAfter = None)
    lock.unlock()
    lock
  }

  class ListReader(val list: List[String]) extends DataInputStream {
    var iterator = list.iterator


    override def readNextValue(): Future[Either[DataStreamState, String]] = {
      Future {
        maybeLock { () => if (iterator.hasNext) {
          Right(iterator.next())
        } else {
          Left(NoMoreData)
        }
        }

      }
    }

    override def close(): Unit = {

    }
  }

  class Writer(val path: Seq[String]) extends DataOutputStream {
    var values = List[String]()

    override def writeNextValue(evalue: String): Unit = {

      maybeLock { () => {
        synchronized {
          values = values :+ evalue
          storedValues = storedValues + (path -> values)
        }
      }
      }

    }

    override def close(): Unit = {
      storedValues = storedValues + (path -> values)
    }
  }

}


