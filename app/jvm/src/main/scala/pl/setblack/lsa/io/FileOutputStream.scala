package pl.setblack.lsa.io

import java.io.ObjectOutputStream

import pl.setblack.lsa.io.DataStorage.DataOutputStream

class FileOutputStream(val outStream: ObjectOutputStream) extends DataOutputStream {
  override def writeNextValue(value: String): Unit = {
    synchronized {
      try {
        println(s"writing ${value.length}")
        outStream.writeObject(value)
      } catch {
        case e: Throwable => {
          println(s"--- PROBLEM Writing content of size----\n${value.length}\n----END----")
          e.printStackTrace()
        }
      }
    }
  }

  override def close(): Unit = outStream.close()
}


