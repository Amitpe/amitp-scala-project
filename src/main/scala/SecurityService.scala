import api.Types.{CloudServiceName, IP}

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
