package services

import api.Types.{CloudServiceName, IP}
import common.Async
import io.FileReader

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps


class DefaultCloudServicesUsageFinder(logEntriesHandler: LogEntriesHandler,
                                      fileReader: FileReader,
                                      concurrency: Int = 100) extends CloudServicesUsageFinder {

  private implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(concurrency))
  private val counter = new AtomicInteger(0)

  def findCloudServicesUsages(): Map[CloudServiceName, Set[IP]] =
    try {
      val linesBuffer = fileReader.getLines()

      val cloudNamesToIpsTasks = extractCloudNamesToIpsAsync(linesBuffer)

      val cloudNamesToIpsTask = Future.sequence(cloudNamesToIpsTasks)

      val cloudNamesToIps = Async.waitForCompletion(cloudNamesToIpsTask)

      println(counter)

      buildResultMap(cloudNamesToIps)
    } finally {
      releaseResources()
    }

  private def extractCloudNamesToIpsAsync(linesBuffer: Iterator[String]) = {
    linesBuffer.map { line =>
      Future {
//        Thread.sleep(100)
        counter.incrementAndGet()
        logEntriesHandler.extractCloudAndUserIp(line)
      }
    }
  }

  //  def extractCloudNamesToIpsAsync(linesBuffer: Iterator[String])(implicit ec: ExecutionContext): Iterator[Future[Option[(CloudServiceName, IP)]]] = {
  //    // Use takeWhile to ensure we only create Futures while there are lines left to process
  //    Iterator.continually {
  //      if (linesBuffer.hasNext) {
  //        Future {
  //          val line = if (linesBuffer.hasNext) Try(linesBuffer.next()).getOrElse("") else ""
  //
  //          val threadName = Thread.currentThread().getName
  //          println(s"Processing on thread: $threadName")
  //
  //          counter.incrementAndGet()
  //          logEntriesHandler.extractCloudAndUserIp(line)
  //        }
  //      } else {
  //        Future.successful(None)
  //      }
  //    }.takeWhile(_ => linesBuffer.hasNext)
  //  }

  private def buildResultMap(domainsToIps: Iterator[Option[(CloudServiceName, IP)]]) = {
    domainsToIps.foldLeft(Map.empty[CloudServiceName, Set[CloudServiceName]]) {
      case (acc, Some((key, value))) =>
        acc.updated(key, acc.getOrElse(key, Set.empty) + value)
      case (acc, None) =>
        acc // Skip None values
    }
  }

  private def releaseResources(): Unit = {
    ec match {
      case executor: ExecutorService =>
        fileReader.close()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
      case _ =>
    }
  }
}
