package services

import api.Types.{CloudServiceName, IP}

trait CloudServicesUsageFinder {
  def findCloudServicesUsages(): Map[CloudServiceName, Set[IP]]
}
