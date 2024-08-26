import api.Types.{CloudServiceName, IP}

trait SecurityService {
  /*
    Provides a list of distinct internal IPs that have used cloud services.
   */
  def getCloudServiceUsage(): Map[CloudServiceName, Set[IP]]

  /*
  Prints to the console a list of distinct internal IPs that have used cloud services.
 */
  def printCloudServiceUsage(): Unit
}
