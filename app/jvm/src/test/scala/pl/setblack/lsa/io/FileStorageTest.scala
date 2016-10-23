package pl.setblack.lsa.io

import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.Future

class FileStorageTest extends AsyncFunSpec with Matchers {
  describe("writer") {
    val storage = new FileStorage("target/tests")
    val path = Seq("test", "simple")
    it("should open  file to write") {
      val writer = storage.openDataWriter(path)
      writer.map(file => {
        file should not be (null)
      })
    }

    it("should store  simple text") {
      val writer = storage.openDataWriter(path)
      writer.map(file => {
        file.writeNextValue("sampleText")
        file.close()
        file should not be (null)
      })
    }

    it("should store  and read simple text") {
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
  }
}
