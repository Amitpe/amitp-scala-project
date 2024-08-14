import Types.{CloudServiceName, IP}

import scala.collection.mutable
import scala.io.Source
import scala.util.matching.Regex

class CloudServicesUsageFinder(firewallFileName: String) {
  private val csvReader = new CloudServicesCSVProvider()
  private val domainToServiceName: Map[String, String] = csvReader.provideCloudServicesMap()
  private val cloudNameToUniqueIps: mutable.Map[String, mutable.Set[String]] = mutable.Map.empty

  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    val firewallFileBuffer = Source.fromFile(firewallFileName)

    try {
      for (line <- firewallFileBuffer.getLines()) {
        if (line != "") {
          parseLogLine(line).foreach { logEntry =>
            val domain = logEntry.domain.get // handle case not found

            domainToServiceName
              .get(domain)
              .map(cloudName => addIpToCloud(cloudName, logEntry.internalIp))
          }
        }
      }
    } finally {
      firewallFileBuffer.close()
    }

    cloudNameToUniqueIps
  }

  def addIpToCloud(cloudName: String, ip: IP): Unit = {
    val ips = cloudNameToUniqueIps.getOrElseUpdate(cloudName, mutable.Set.empty)
    ips += ip
  }

  def parseLogLine(line: String): Option[LogEntry] = {
    val parts = line.split(" ")
    val direction = parts(6) // Should be INBOUND or OUTG CONN

    // Extract IP based on direction
    val ip = direction match {
      case "INBOUND" => parts.find(_.startsWith("DST=")).map(_.split("=")(1))
      case "OUTG" => parts.find(_.startsWith("SRC=")).map(_.split("=")(1))
      case _ => None
    }

    // Extract domain if present
    val domain = parts.find(_.startsWith("DOMAIN=")).map(_.split("=")(1))

    ip.map(LogEntry(_, domain))
  }

  def parseLogLineRegex(line: String): Option[LogEntry] = {
    // Regular expression to extract the DST IP
    val outboundPattern: Regex = """OUTG CONN.*?SRC=(\d+\.\d+\.\d+\.\d+).*?DOMAIN=([^ ]+)""".r
    val inboundPattern: Regex = """INBOUND.*?DST=(\d+\.\d+\.\d+\.\d+).*?DOMAIN=([^ ]+)""".r

    line match {
      case outboundPattern(ip, domain) =>
        Some(LogEntry(ip, Option(domain)))
      case inboundPattern(ip, domain) =>
        Some(LogEntry(ip, Option(domain)))
      case _ => None
    }
  }

}


case class LogEntry(internalIp: String, domain: Option[String])

object Types {
  type CloudServiceName = String
  type IP = String
}







