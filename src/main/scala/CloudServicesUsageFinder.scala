import Types.{CloudServiceName, IP}

import scala.collection.mutable
import scala.io.Source

class CloudServicesUsageFinder(pathToFirewallLogFile: String) {
  private val csvReader = new CloudServicesCSVProvider()
  private val parser = new FirewallParser()

  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()
  private val cloudNameToUniqueIps: mutable.Map[String, mutable.Set[String]] = mutable.Map.empty

  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    val firewallFileBuffer = Source.fromFile(pathToFirewallLogFile)

    try {
      for (line <- firewallFileBuffer.getLines()) {
        if (line != "") {
          parseAndAccumulate(line)
        }
      }
    } finally {
      firewallFileBuffer.close()
    }

    cloudNameToUniqueIps
  }

  private def parseAndAccumulate(line: String): Unit = {
    parser.parseLogLine(line).foreach { logEntry =>
      val domain = logEntry.domain.get // TODO: handle case not found

      domainToServiceName
        .get(domain)
        .map(cloudName => addIpToCloud(cloudName, logEntry.internalIp))
    }
  }

  def addIpToCloud(cloudName: String, ip: IP): Unit = {
    val ips: mutable.Set[CloudServiceName] = cloudNameToUniqueIps.getOrElseUpdate(cloudName, mutable.Set.empty)
    ips += ip
  }

}


case class LogEntry(internalIp: String, domain: Option[String])

object Types {
  type CloudServiceName = String
  type IP = String
}







