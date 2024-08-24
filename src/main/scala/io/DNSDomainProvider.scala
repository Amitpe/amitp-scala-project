package io

import java.net.InetAddress

trait DNSDomainProvider {
  def getDomainFromIP(ip: String): Option[String]
}

class JavaInetDNSDomainProvider extends DNSDomainProvider {
  override def getDomainFromIP(ip: String): Option[String] = {
    getDomain(ip)
  }

  private def getDomain(ip: String) = {
    try {
      val inetAddress = InetAddress.getByName(ip)
      Some(inetAddress.getHostName)
    } catch {
      case e: Exception =>
        println(s"Error performing reverse DNS lookup: ${e.getMessage}")
        None
    }
  }
}
