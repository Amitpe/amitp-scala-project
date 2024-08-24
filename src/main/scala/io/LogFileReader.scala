package io

import scala.io.Source

trait FileReader {
  def getLines(): Iterator[String]

  def close(): Unit
}

class LogFileReader(path: String) extends FileReader {

  override def getLines(): Iterator[String] = {
    val bufferedIterator = Source.fromFile(path)
    bufferedIterator.getLines()
  }

  override def close(): Unit = ()
//    bufferedIterator.close()
}
