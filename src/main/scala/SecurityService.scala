import FileSystemSecurityService._
import common.{Filter, FirewallParser, LruCache, Parser}
import filter.CombinedFilter
import io.{DNSDomainProvider, FileReader, JavaInetDNSDomainProvider, LogFileReader}
import api.Types.{CloudServiceName, IP}
import services.{CachingDNSService, CloudServicesUsageFinder}

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


/*
  Dependency injection is possible through here.
  if a dependency is not provided we use the default one which is defined in the Object below.
 */
class FileSystemSecurityService(
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
    val cache = new LruCache[String, Option[String]](maxSize = DEFAULT_CACHE_SIZE, expireAfterWriteMinutes = DEFAULT_EXPIRE_AFTER_WRITE_MINUTES)
    val DNSService = new CachingDNSService(DNSDomainProvider, cache)
    val filters = maybeFilters.getOrElse(DEFAULT_FILTERS)
    val combinedFilter = new CombinedFilter(filters)
    val parser = maybeParser.getOrElse(new FirewallParser())
    val fileReader = maybeFileReader.getOrElse(new LogFileReader(maybePathToFirewallLogFile.getOrElse(DEFAULT_PATH_TO_FIREWALL_LOG_FILE)))

    new CloudServicesUsageFinder(DNSService, combinedFilter, parser, fileReader)
  }
}

object FileSystemSecurityService {
  private val DEFAULT_PATH_TO_FIREWALL_LOG_FILE = "src/main/resources/firewall.log"
  private val DEFAULT_DNS_DOMAIN_PROVIDER = new JavaInetDNSDomainProvider
  private val DEFAULT_CACHE_SIZE = 1000
  private val DEFAULT_EXPIRE_AFTER_WRITE_MINUTES = 5
  private val DEFAULT_FILTERS = Seq.empty
}