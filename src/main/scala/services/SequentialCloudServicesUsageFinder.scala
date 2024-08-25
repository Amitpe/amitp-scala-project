package services

import api.LogEntry
import api.Types.{CloudServiceName, IP}
import common.{Filter, Parser}
import io.{CloudServicesCSVProvider, FileReader}

import scala.collection.mutable

class SequentialCloudServicesUsageFinder(DNSService: DNSService,
                                         combinedFilter: Filter,
                                         parser: Parser,
                                         fileReader: FileReader) {
  private val csvReader = new CloudServicesCSVProvider()

  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()
  private val cloudServicesToUniqueIps: mutable.Map[String, mutable.Set[String]] = mutable.Map.empty
  private val EMPTY_STRING = ""

  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    try {
      for (line <- fileReader.getLines()) {
        if (line != EMPTY_STRING) {
          parser
            .parseLogLine(line)
            .filter(isEntryAllowed)
            .foreach(accumulate)
        }
      }
    } finally {
      fileReader.close()
    }

    cloudServicesToUniqueIps
  }

  private def isEntryAllowed(logEntry: LogEntry) = {
    combinedFilter.isAllowed(logEntry)
  }

  private def accumulate(logEntry: LogEntry): Unit = {
    val domain = logEntry.domain.getOrElse(DNSService.getDomainFromIP(logEntry.cloudIp).getOrElse(EMPTY_STRING))

    domainToServiceName
      .get(domain)
      .foreach(cloudName => addIpToCloud(cloudName, logEntry.userIp))
  }

  def addIpToCloud(cloudName: String, ip: IP): Unit = {
    val ips: mutable.Set[CloudServiceName] = cloudServicesToUniqueIps.getOrElseUpdate(cloudName, mutable.Set.empty)
    ips += ip
  }


}








