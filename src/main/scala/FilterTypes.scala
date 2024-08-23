import FilterTypes.{EXCLUDE, FILTER_TYPE, INCLUDE}
import io.netty.handler.ipfilter.{IpFilterRuleType, IpSubnetFilterRule}

import java.net.InetSocketAddress

trait Filter {
  def isAllowed(logEntry: LogEntry): Boolean // true - allow
}

object FilterTypes {
  sealed trait FILTER_TYPE

  case object INCLUDE extends FILTER_TYPE

  case object EXCLUDE extends FILTER_TYPE
}


class IpFilter(ip: String,
               range: Option[Int],
               filterType: FILTER_TYPE) extends Filter {

  private final val NO_RANGE = 32
  private val subnetRule = new IpSubnetFilterRule(ip, range.getOrElse(NO_RANGE), filterType match {
    case INCLUDE => IpFilterRuleType.ACCEPT
    case EXCLUDE => IpFilterRuleType.REJECT
    case _ => throw new RuntimeException("Invalid filter type")
  })

  override def isAllowed(logEntry: LogEntry): Boolean = {
    val address = new InetSocketAddress(logEntry.userIp, 0)
    subnetRule.matches(address)
  }
}



