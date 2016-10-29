package pl.setblack.lsa.events

import pl.setblack.lsa.io.DataStorage.{DataInputStream, DataOutputStream, DataStorage}

import scala.concurrent.{ExecutionContext, Future}

class FakeStorage(implicit val executionContext: ExecutionContext) extends DataStorage{
  override def openDataReader(path: Seq[String]): Future[Option[DataInputStream]] = Future { None }

  override def openDataWriter(path: Seq[String]): Future[DataOutputStream] = Future {
    EmptyOutput
  }
}

object EmptyOutput extends DataOutputStream {
  override def writeNextValue(value: String): Unit = {
    println (s"writing=${value}")
  }

  override def close(): Unit = {}
}
