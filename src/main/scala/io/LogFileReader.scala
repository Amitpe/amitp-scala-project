package io

import scala.io.Source

trait FileReader {
  def getLines(): Iterator[String]

  def close(): Unit
}

class LogFileReader(path: String) extends FileReader {
  val bufferedIterator = Source.fromFile(path)

  override def getLines(): Iterator[String] = {
    bufferedIterator.getLines()
  }

  override def close(): Unit = ()
//    bufferedIterator.close()
}
