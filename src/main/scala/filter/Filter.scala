package common

import services.LogEntry

trait Filter {
  def isAllowed(logEntry: LogEntry): Boolean // true - allow
}

object FilterTypes {
  sealed trait FILTER_TYPE

  case object INCLUDE extends FILTER_TYPE

  case object EXCLUDE extends FILTER_TYPE
}



