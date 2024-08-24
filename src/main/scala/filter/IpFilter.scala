package filter

import common.Filter
import common.FilterTypes.{EXCLUDE, FILTER_TYPE, INCLUDE}
import io.netty.handler.ipfilter.{IpFilterRuleType, IpSubnetFilterRule}
import services.LogEntry

import java.net.InetSocketAddress

class IpFilter(ip: String,
               range: Option[Int] = None,
               filterType: FILTER_TYPE) extends Filter {

  private final val NO_RANGE = 32
  private val subnetRule = new IpSubnetFilterRule(ip, range.getOrElse(NO_RANGE), filterType match {
    case INCLUDE => IpFilterRuleType.ACCEPT
    case EXCLUDE => IpFilterRuleType.REJECT
    case _ => throw new RuntimeException("Invalid filter type")
  })

  override def isAllowed(logEntry: LogEntry): Boolean = {
    val address = new InetSocketAddress(logEntry.userIp, 0)
    if (filterType == INCLUDE)
      subnetRule.matches(address)
    else
      !subnetRule.matches(address)
  }
}
