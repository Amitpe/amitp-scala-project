package services
import api.Types.{CloudServiceName, IP}

import scala.collection.mutable

class ConcurrentCloudServicesUsageFinder extends CloudServicesUsageFinder {

  override def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]] = ???
}
