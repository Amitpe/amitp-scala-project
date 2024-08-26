package services

import api.LogEntry
import api.Types.{CloudServiceName, IP}
import common.{Filter, Parser}
import io.CloudServicesCSVProvider

class LogEntriesHandler(DNSService: DNSService,
                        combinedFilter: Filter,
                        parser: Parser) {

  private val csvReader = new CloudServicesCSVProvider()
  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()
  private val EMPTY_STRING = ""

  def extractCloudAndUserIp(line: String): Option[(CloudServiceName, IP)] =
    if (line != EMPTY_STRING)
      parser.parseLogLine(line)
        .filter(isEntryAllowed)
        .flatMap(maybeCloudNameToIp)
    else
      None

  private def isEntryAllowed(logEntry: LogEntry): Boolean = {
    combinedFilter.isAllowed(logEntry)
  }

  private def maybeCloudNameToIp(logEntry: LogEntry): Option[(CloudServiceName, CloudServiceName)] = {
    val domain = logEntry.domain.getOrElse {
      DNSService.getDomainFromIP(logEntry.cloudIp).getOrElse(EMPTY_STRING)
    }

    domainToServiceName
      .get(domain)
      .map(cloudName => (cloudName, logEntry.userIp))
  }

}
