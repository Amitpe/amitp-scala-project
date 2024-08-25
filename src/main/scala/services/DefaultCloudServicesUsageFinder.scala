package services

import api.LogEntry
import api.Types.{CloudServiceName, IP}
import common.{Filter, Parser}
import io.{CloudServicesCSVProvider, FileReader}

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


class DefaultCloudServicesUsageFinder(DNSService: DNSService,
                                      combinedFilter: Filter,
                                      parser: Parser,
                                      fileReader: FileReader,
                                      concurrency: Int = Runtime.getRuntime.availableProcessors()) {

  private implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(concurrency))

  private val csvReader = new CloudServicesCSVProvider()
  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()

  private val cloudServicesToUniqueIps: mutable.Map[String, mutable.Set[String]] = new TrieMap[String, mutable.Set[String]]()
  private val EMPTY_STRING = ""

  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    try {
      val linesBuffer = fileReader.getLines()

      val tasks = handleLogLinesAsync(linesBuffer)

      val aSingleFuture = Future.sequence(tasks)

      releaseResourcesWhenTasksAreCompleted(aSingleFuture)

      waitForTaskCompletion(aSingleFuture)

      cloudServicesToUniqueIps

    } finally {
      releaseResources()
    }
  }

  private def handleLogLinesAsync(linesBuffer: Iterator[CloudServiceName]) = {
    linesBuffer.map { line =>
      Future {
        handleLogLine(line)
      }
    }
  }

  private def handleLogLine(line: CloudServiceName): Unit = {
    if (line != EMPTY_STRING) {
      parser.parseLogLine(line)
        .filter(isEntryAllowed)
        .foreach(accumulate)
    }
  }

  private def isEntryAllowed(logEntry: LogEntry): Boolean = {
    combinedFilter.isAllowed(logEntry)
  }

  private def accumulate(logEntry: LogEntry): Unit = {
    val domain = logEntry.domain.getOrElse(
      DNSService.getDomainFromIP(logEntry.cloudIp).getOrElse(EMPTY_STRING)
    )

    domainToServiceName
      .get(domain)
      .foreach(cloudName => addIpToCloud(cloudName, logEntry.userIp))
  }

  private def addIpToCloud(cloudName: String, ip: IP): Unit = {
    val ips: mutable.Set[String] = cloudServicesToUniqueIps.getOrElseUpdate(cloudName, mutable.Set.empty)
    ips += ip
  }

  private def releaseResourcesWhenTasksAreCompleted(allFutures: Future[Iterator[Unit]]): Unit = {
    allFutures.onComplete {
      case Success(_) => fileReader.close()
      case Failure(exception) =>
        fileReader.close()
        throw exception
    }
  }

  private def waitForTaskCompletion(aSingleFuture: Future[Iterator[Unit]]) = {
    Await.result(aSingleFuture, 5.minutes)
  }

  private def releaseResources(): Unit = {
    fileReader.close()
    ec match {
      case executor: ExecutorService =>
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
      case _ =>
    }
  }
}
