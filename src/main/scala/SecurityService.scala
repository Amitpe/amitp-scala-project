import FileSystemSecurityService.{DEFAULT_CACHE_SIZE, DEFAULT_DNS_DOMAIN_PROVIDER, DEFAULT_EXPIRE_AFTER_WRITE_MINUTES, DEFAULT_PATH_TO_FIREWALL_LOG_FILE}
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


/*
  Dependency injection is possible through here.
  if a dependency is not provided we use the default one which is defined in the Object below.
 */
class FileSystemSecurityService(
                                 maybePathToFirewallLogFile: Option[String] = None,
                                 maybeDNSDomainProvider: Option[DNSDomainProvider] = None
                               ) extends SecurityService {
  private val pathToFirewallLogFile = maybePathToFirewallLogFile.getOrElse(DEFAULT_PATH_TO_FIREWALL_LOG_FILE)
  private val DNSDomainProvider = maybeDNSDomainProvider.getOrElse(DEFAULT_DNS_DOMAIN_PROVIDER)
  private val cache = new LruCache[String, Option[String]](maxSize = DEFAULT_CACHE_SIZE, expireAfterWriteMinutes = DEFAULT_EXPIRE_AFTER_WRITE_MINUTES)
  private val DNSService = new CachingDNSService(DNSDomainProvider, cache)
  private val cloudServicesUsageFinder = new CloudServicesUsageFinder(DNSService, pathToFirewallLogFile)

  override def getCloudServiceUsage(): mutable.Map[CloudServiceName, mutable.Set[IP]] = {
    cloudServicesUsageFinder.findCloudServicesUsages()
  }

  override def printCloudServiceUsage(): Unit = {
    println(cloudServicesUsageFinder.findCloudServicesUsages())
  }
}

object FileSystemSecurityService {
  val DEFAULT_PATH_TO_FIREWALL_LOG_FILE = "src/main/resources/firewall.log"
  val DEFAULT_DNS_DOMAIN_PROVIDER = new JavaInetDNSDomainProvider
  val DEFAULT_CACHE_SIZE = 1000
  val DEFAULT_EXPIRE_AFTER_WRITE_MINUTES = 5
}