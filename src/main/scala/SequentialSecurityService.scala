import SequentialSecurityService.{DEFAULT_DNS_DOMAIN_PROVIDER, DEFAULT_FILTERS, DEFAULT_PATH_TO_FIREWALL_LOG_FILE}
import api.Types.{CloudServiceName, IP}
import common.{Filter, FirewallParser, LruCache, Parser}
import filter.CombinedFilter
import io.{DNSDomainProvider, FileReader, JavaInetDNSDomainProvider, LogFileReader}
import services.{CachingDNSService, CloudServicesUsageFinder}

import scala.collection.mutable

class SequentialSecurityService(
                                 maybePathToFirewallLogFile: Option[String] = None,
                                 maybeParser: Option[Parser] = None,
                                 maybeDNSDomainProvider: Option[DNSDomainProvider] = None,
                                 maybeFilters: Option[Seq[Filter]] = None,
                                 maybeFileReader: Option[FileReader] = None
                               ) extends SecurityService {
  private val cloudServicesUsageFinder = buildCloudServicesUsageFinder()

  override def getCloudServiceUsage(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    cloudServicesUsageFinder.findCloudServicesUsages()
  }

  override def printCloudServiceUsage(): Unit = {
    println(cloudServicesUsageFinder.findCloudServicesUsages())
  }

  private def buildCloudServicesUsageFinder(): CloudServicesUsageFinder = {
    val DNSDomainProvider = maybeDNSDomainProvider.getOrElse(DEFAULT_DNS_DOMAIN_PROVIDER)
    val cache = LruCache.aCacheForMediumCompany[String, Option[String]]()
    val DNSService = new CachingDNSService(DNSDomainProvider, cache)
    val filters = maybeFilters.getOrElse(DEFAULT_FILTERS)
    val combinedFilter = new CombinedFilter(filters)
    val parser = maybeParser.getOrElse(new FirewallParser())
    val fileReader = maybeFileReader.getOrElse(new LogFileReader(maybePathToFirewallLogFile.getOrElse(DEFAULT_PATH_TO_FIREWALL_LOG_FILE)))

    new CloudServicesUsageFinder(DNSService, combinedFilter, parser, fileReader)
  }
}

object SequentialSecurityService {
  private val DEFAULT_PATH_TO_FIREWALL_LOG_FILE = "src/main/resources/firewall.log"
  private val DEFAULT_DNS_DOMAIN_PROVIDER = new JavaInetDNSDomainProvider
  private val DEFAULT_FILTERS = Seq.empty
}