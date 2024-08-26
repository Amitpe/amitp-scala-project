package services

import api.Types.{CloudServiceName, IP}
import common.Async
import io.FileReader

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps

class DefaultCloudServicesUsageFinder(logEntriesHandler: LogEntriesHandler,
                                      fileReader: FileReader,
                                      concurrency: Int = Runtime.getRuntime.availableProcessors()) extends CloudServicesUsageFinder {

  private implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(concurrency))

  def findCloudServicesUsages(): Map[CloudServiceName, Set[IP]] =
    try {
      val linesBuffer = fileReader.getLines()

      val cloudNameToIpsTasks = extractCloudNamesToIpsAsync(linesBuffer)

      val cloudNamesToIpsTask = Future.sequence(cloudNameToIpsTasks)

      val cloudNamesToIps = Async.waitForCompletion(cloudNamesToIpsTask)

      buildResultMap(cloudNamesToIps)
    } finally {
      releaseResources()
    }

  private def extractCloudNamesToIpsAsync(linesBuffer: Iterator[CloudServiceName]) = {
    linesBuffer.map { line =>
      Async.task {
        logEntriesHandler.extractCloudAndUserIp(line)
      }
    }
  }

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
