package pl.setblack.lsa.io

import java.io.IOException

import scala.concurrent.Future






object DataStorage {

  sealed trait DataStreamState

  case object NoMoreData extends DataStreamState

  case class DataError(ex: IOException) extends DataStreamState

  trait DataStorage {
    def openDataReader(path: Seq[String]): Future[Option[DataInputStream]]

    def openDataWriter(path: Seq[String]): Future[DataOutputStream]
  }

  trait DataInputStream {
    def readNextValue(): Future[Either[DataStreamState, String]] //TODO: Erik- Meijer tuta

    def close(): Unit
  }

  trait DataOutputStream {
    def writeNextValue(value: String): Unit

    def close(): Unit
  }

}