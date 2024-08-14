import FileSystemSecurityService.DEFAULT_PATH_TO_FIREWALL_LOG_FILE
import Types.{CloudServiceName, IP}

import scala.collection.mutable

trait SecurityService {
/*
  Provides a list of distinct internal IPs that have used cloud services.
 */
  def getCloudServiceUsage(): mutable.Map[CloudServiceName, mutable.Set[IP]]

  /*
  Prints to the console a list of distinct internal IPs that have used cloud services.
 */
  def printCloudServiceUsage(): Unit
}


class FileSystemSecurityService(maybePathToFirewallLogFile: Option[String]) extends SecurityService {
  val pathToFirewallLogFile = maybePathToFirewallLogFile.getOrElse(DEFAULT_PATH_TO_FIREWALL_LOG_FILE)
  val cloudServicesUsageFinder = new CloudServicesUsageFinder(pathToFirewallLogFile)

  override def getCloudServiceUsage(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    cloudServicesUsageFinder.findCloudServicesUsages()
  }

  override def printCloudServiceUsage(): Unit = {
    println(cloudServicesUsageFinder.findCloudServicesUsages())
  }
}

object FileSystemSecurityService {
  val DEFAULT_PATH_TO_FIREWALL_LOG_FILE = "src/main/resources/firewall.log"
}