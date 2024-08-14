

trait DNSService {
  def getDomainFromIP(ip: String): Option[String]
}

class CachingDNSService(DNSDomainProvider: DNSDomainProvider,
                        cache: LruCache[String, Option[String]]) extends DNSService {

  override def getDomainFromIP(ip: String): Option[String] = {
    cache.getOrElseUpdate(ip, () => DNSDomainProvider.getDomainFromIP(ip))
  }

}
