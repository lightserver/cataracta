package pl.setblack.lsa.io

import java.io.ObjectOutputStream

import pl.setblack.lsa.io.DataStorage.DataOutputStream

class FileOutputStream(val outStream : ObjectOutputStream) extends DataOutputStream{
  override def writeNextValue(value: String): Unit = {
    outStream.writeUTF(value)
  }

  override def close(): Unit = outStream.close()
}


