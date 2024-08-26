package services

import api.Types.{CloudServiceName, IP}
import io.FileReader

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps

class DefaultCloudServicesUsageFinder(logEntriesHandler: LogEntriesHandler,
                                      fileReader: FileReader,
                                      concurrency: Int = Runtime.getRuntime.availableProcessors()) extends CloudServicesUsageFinder {

  private implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(concurrency))

  def findCloudServicesUsages(): Map[CloudServiceName, Set[IP]] =
    try {
      val linesBuffer = fileReader.getLines()
      val domainsToIpsFutures = linesBuffer.map { line =>
        Future {
          logEntriesHandler.extractCloudAndUserIp(line)
        }
      }
      val domainsToIpsFuture = Future.sequence(domainsToIpsFutures)
      val domainsToIps: Iterator[Option[(CloudServiceName, IP)]] = waitForTaskCompletion(domainsToIpsFuture)

      val result = domainsToIps.foldLeft(Map.empty[String, Set[String]]) {
        case (acc, Some((key, value))) =>
          acc.updated(key, acc.getOrElse(key, Set.empty) + value)
        case (acc, None) =>
          acc // Skip None values
      }

      result
    } finally {
      releaseResources()
    }

  private def waitForTaskCompletion[T](aSingleFuture: Future[Iterator[T]]): Iterator[T] = {
    Await.result(aSingleFuture, 5.minutes)
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
