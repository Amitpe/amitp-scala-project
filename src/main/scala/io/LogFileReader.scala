package io

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source

trait FileReader {
  def getLines(): Iterator[String]

  def close(): Unit
}

class LogFileReader(path: String) extends FileReader {
  private val bufferedIterator = Source.fromFile(path)

  override def getLines(): Iterator[String] = {
    bufferedIterator.getLines()
  }

  override def close(): Unit =
    bufferedIterator.close()
}
