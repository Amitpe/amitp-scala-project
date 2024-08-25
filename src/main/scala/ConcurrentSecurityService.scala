import api.Types.{CloudServiceName, IP}

import scala.collection.mutable

class ConcurrentSecurityService extends SecurityService {

  override def getCloudServiceUsage(): mutable.Map[CloudServiceName, mutable.Set[IP]] = ???

  override def printCloudServiceUsage(): Unit = ???
}
