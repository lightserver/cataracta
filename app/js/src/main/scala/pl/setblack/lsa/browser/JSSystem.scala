package pl.setblack.lsa.browser

import pl.setblack.lsa.boot.GenericSystem
import pl.setblack.lsa.concurrency.NoConcurrencySystem
import pl.setblack.lsa.events.Node
import pl.setblack.lsa.io.DataStorage.{DataInputStream, DataOutputStream, DataStorage}
import pl.setblack.lsa.io.Storage
import pl.setblack.lsa.os.SimpleReality
import pl.setblack.lsa.resources.JSResources
import pl.setblack.lsa.security.SignedCertificate

import scala.concurrent.{ExecutionContext, Future, Promise}

class JSSystem (val clientId: Future[Long],rootCertificate : Option[SignedCertificate])
               (implicit  executionContext: ExecutionContext)
  extends GenericSystem(rootCertificate){


  override def createMainNode(): Node = {

    implicit  val reality = SimpleReality(createStorage, new NoConcurrencySystem, createSecurityProvider, new JSResources())
    val node = new Node(clientId)(reality)
    node
  }

  private def createStorage(): DataStorage = {
    class NullStorage extends DataStorage {
      override def openDataReader(path: Seq[String]): Future[Option[DataInputStream]] = {
        Future { None }
      }

      override def openDataWriter(path: Seq[String]): Future[DataOutputStream] = {
        Future {
          new NullWriter
        }
      }
    }

    class NullWriter extends DataOutputStream {
      override def writeNextValue(value: String): Unit = {}

      override def close(): Unit = {}
    }
    return new NullStorage
  }
}
