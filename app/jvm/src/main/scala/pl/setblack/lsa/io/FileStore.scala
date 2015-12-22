package pl.setblack.lsa.io

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, OpenOption, Files, Paths}

import upickle.default._

class FileStore(val diskPath: String) extends Storage {
  override def save(value: String, path: Seq[String]): Unit = {
    val fsPath = createPath(path)
    if (!Files.exists(fsPath.getParent)) {
      Files.createDirectories(fsPath.getParent)
    }
    val output = Files.newBufferedWriter(fsPath, StandardOpenOption.CREATE )
    output.write(value)
    output.close()

  }

  override def load(path: Seq[String]): Option[String] = {
    val fsPath = createPath(path)
    if (Files.exists(fsPath)) {
      val input = Files.newBufferedReader(fsPath)
      val line = input.readLine()
      input.close()
      println("I have read:"+ line)
      Some(line)
    } else {
      None
    }

  }

  def createPath(path: Seq[String]) = {
    Paths.get(diskPath, path: _*)
  }
}


