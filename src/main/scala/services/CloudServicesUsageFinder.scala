package services

import common.{Filter, FirewallParser}
import io.CloudServicesCSVProvider
import services.Types.{CloudServiceName, IP}

import scala.collection.mutable
import scala.io.Source

class CloudServicesUsageFinder(DNSService: DNSService,
                               combinedFilter: Filter,
                               pathToFirewallLogFile: String) {
  private val csvReader = new CloudServicesCSVProvider()
  private val parser = new FirewallParser()

  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()
  private val cloudServicesToUniqueIps: mutable.Map[String, mutable.Set[String]] = mutable.Map.empty
  private val EMPTY_STRING = ""

  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    val firewallFileBuffer = Source.fromFile(pathToFirewallLogFile)

    try {
      for (line <- firewallFileBuffer.getLines()) {
        if (line != EMPTY_STRING) {
          parser
            .parseLogLine(line)
            .filter(isEntryAllowed)
            .foreach(accumulate)
        }
      }
    } finally {
      firewallFileBuffer.close()
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


case class LogEntry(userIp: String, cloudIp: String, domain: Option[String], userName: Option[String])

object Types {
  type CloudServiceName = String
  type IP = String
}







