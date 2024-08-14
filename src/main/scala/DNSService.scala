

trait DNSService {
  def getDomainFromIP(ip: String): Option[String]
}

class CachingDNSService(DNSDomainProvider: DNSDomainProvider) extends DNSService {

  override def getDomainFromIP(ip: String): Option[String] = {
    // TODO - add caching layer here using GUAVA cache
    DNSDomainProvider.getDomainFromIP(ip)
  }

}
