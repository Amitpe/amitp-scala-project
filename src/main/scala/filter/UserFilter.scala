package filter

import common.Filter
import common.FilterTypes.{EXCLUDE, FILTER_TYPE, INCLUDE}
import api.LogEntry

class UserFilter(regex: String, filterType: FILTER_TYPE) extends Filter {

  private val pattern = regex.r

  override def isAllowed(logEntry: LogEntry): Boolean = {
    val userNameMatches = userNameMatchPattern(logEntry)

    filterType match {
      case INCLUDE => userNameMatches
      case EXCLUDE => !userNameMatches
    }
  }

  private def userNameMatchPattern(logEntry: LogEntry): Boolean =
    logEntry.userName.exists(name => pattern.findFirstIn(name).isDefined)
}
