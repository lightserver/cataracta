package pl.setblack.lsa.io

import java.io.{EOFException, IOException, ObjectInputStream}
import java.nio.file.{Files, Path}

import pl.setblack.lsa.io.DataStorage.{DataError, DataInputStream, DataStreamState, NoMoreData}

import scala.concurrent.{ExecutionContext, Future}


class FileInputStream(val filesIterator : Iterator[Path])(implicit val ectx: ExecutionContext) extends DataInputStream{

  var nextFile = getNextFile()

  private def readNextValueInternal() : Either[DataStreamState, String]= {
    synchronized(
    nextFile.map ( input => {
      try {
        val read = input.readUTF()
        Right(read)
      } catch {
        case eof: EOFException => {
          input.close()
          nextFile = getNextFile()
          readNextValueInternal()
        }
        case  io:IOException => Left(DataError(io))
      }
    }).getOrElse(Left(NoMoreData)))
  }


  override def readNextValue(): Future[Either[DataStreamState, String]]  = {
    Future {
        readNextValueInternal()
    }
  }

  override def close(): Unit = {
    nextFile.map(input => input.close())
  }


  private def getNextFile() = {
    if ( filesIterator.hasNext) {
      val file  = filesIterator.next()
      val inputStream = Files.newInputStream(file)
      Some( new ObjectInputStream(inputStream))
    } else {
      None
    }
  }
}
