package pl.setblack.lsa.io

import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.Future

class FileStorageTest extends AsyncFunSpec with Matchers {
  describe("writer") {
    val storage = new FileStorage("target/tests")
    val path = Seq("test", "simple")

    it("should open  file to write") {
      storage.erase(path)

      val writer = storage.openDataWriter(path)
      writer.map(file => {
        file should not be (null)
      })
    }

    it("should store  simple text") {
      storage.erase(path)

      val writer = storage.openDataWriter(path)
      writer.map(file => {
        file.writeNextValue("sampleText")
        file.close()
        file should not be (null)
      })
    }

    it("should store  and read simple text") {
      storage.erase(path)

      val writer = storage.openDataWriter(path)
      writer.flatMap(outFile => {
        outFile.writeNextValue("sampleText")
        outFile.close()
        val readerFuture = storage.openDataReader(path)
        val futureVal = readerFuture.flatMap(readerOpt => readerOpt.map( reader => reader.readNextValue()).getOrElse(Future{
          Right("bad")}
        ))
        futureVal.map( value => value.right.get should be ("sampleText") )

      })
    }

    it("should store multiple times and read simple text") {
      storage.erase(path)

      val writer = storage.openDataWriter(path)
      writer.flatMap(outFile => {
        outFile.writeNextValue("sampleText1")
        outFile.close()

        val writer2 = storage.openDataWriter(path)
        writer2.flatMap(outFile2 => {
          outFile2.writeNextValue("sampleText2")
          outFile2.close()

          val readerFuture = storage.openDataReader(path)
          val futureVal = readerFuture.flatMap(readerOpt => readerOpt.map( reader => {
            reader.readNextValue()
            reader.readNextValue()
          }).getOrElse(Future{
            Right("bad")}
          ))


          futureVal.map( value => value.right.get should be ("sampleText2") )
        })


      })

    }
  }

  private def cleanFolder(path : Seq[String]): Unit = {
    val storage = new FileStorage("target/tests")
    storage.erase(path)
  }
}
