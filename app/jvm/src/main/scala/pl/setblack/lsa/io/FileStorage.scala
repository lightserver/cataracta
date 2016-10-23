package pl.setblack.lsa.io

import java.io.ObjectOutputStream

import scala.collection.JavaConverters._
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.regex.Pattern

import pl.setblack.lsa.io.DataStorage.{DataInputStream, DataOutputStream, DataStorage}

import scala.concurrent.{ExecutionContext, Future}

class FileStorage(val diskPath: String)(implicit val ectx: ExecutionContext) extends DataStorage {
  val filePattern = Pattern.compile("events_\\d+")
  override def openDataReader(path: Seq[String]): Future[Option[DataInputStream]] = {
    Future {
      val fsPath = createPath(path)
      if (Files.exists(fsPath)) {
        val directoryStream = Files.newDirectoryStream(fsPath)
        val filesInDirectory = directoryStream.iterator().asScala.toList

        val sorted =   ( filesInDirectory
          filter( file => filePattern.matcher(file.getFileName.toString).matches())
          map(file => (file.getFileName.toString.substring("events_".length),file))
          map{ case (numer, file) => (numer.toInt, file) }
          sortBy{ case (numer, file) => numer}
          map{ case (numer, file) => file }
          )

        Some(new FileInputStream(sorted.iterator))
      } else {
        None
      }
    }
  }

  override def openDataWriter(path: Seq[String]): Future[DataOutputStream] = {
    Future {
      val fsPath = createPath(path)
      if (!Files.exists(fsPath)) {
        Files.createDirectories(fsPath)
      }
      val directoryStream = Files.newDirectoryStream(fsPath)
      val filesInDirectory = directoryStream.iterator().asScala.toList
      val maxIndex = ( ( filesInDirectory
          filter( file => filePattern.matcher(file.getFileName.toString).matches())
          map(file => file.getFileName.toString.substring("events_".length))
          map(_.toInt) ) :+ 0
          max
        )
       val newFile = fsPath.resolve("events_" +(maxIndex + 1))
       val outputStream = Files.newOutputStream(newFile, StandardOpenOption.CREATE_NEW)
       val objectOut  = new ObjectOutputStream(outputStream)
      new FileOutputStream(objectOut)
    }
  }

  def createPath(path: Seq[String]) = {
    Paths.get(diskPath, path: _*)
  }
}
