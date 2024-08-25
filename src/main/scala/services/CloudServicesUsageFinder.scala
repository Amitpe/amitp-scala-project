package services

import api.Types.{CloudServiceName, IP}

import scala.collection.mutable

trait CloudServicesUsageFinder {
  def findCloudServicesUsages(): mutable.Map[CloudServiceName, mutable.Set[IP]]
}
