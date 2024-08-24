package filter

import common.Filter
import services.LogEntry

class CombinedFilter(filters: Seq[Filter]) extends Filter {

  def isAllowed(logEntry: LogEntry): Boolean =
    filters.isEmpty || allFiltersAcceptEntry(logEntry)

  private def allFiltersAcceptEntry(logEntry: LogEntry): Boolean =
    filters.forall(filter => filter.isAllowed(logEntry))
}
