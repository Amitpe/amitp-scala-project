import TestFixtures.aRandomString
import api.LogEntry
import common.{Filter, Parser}
import io.{DNSDomainProvider, FileReader, LogFileReader}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class TestBase extends SpecificationWithJUnit with Mockito {

  trait BaseContext extends Scope {
    val DNSProviderMock = mock[DNSDomainProvider]
    val parserMock = mock[Parser]
    val fileReaderMock: FileReader = mock[FileReader]

    fileReaderMock.getLines() returns Iterator.continually(aRandomString()).take(10)

    def givenLogEntries(logEntries: LogEntry*) = {
      when(parserMock.parseLogLine(any())).thenAnswer(new Answer[Option[LogEntry]] {
        private var callCount = 0

        override def answer(invocation: InvocationOnMock): Option[LogEntry] = {
          // Return the appropriate value based on the call count
          val result = if (callCount < logEntries.size) Some(logEntries(callCount)) else None
          callCount += 1
          result
        }
      })
    }

    def givenLogEntry(logEntry: LogEntry): LogEntry = {
      givenLogEntry(Some(logEntry))
      logEntry
    }

    def givenLogEntry(logEntry: Option[LogEntry]) =
      parserMock.parseLogLine(any()) returns logEntry

    def givenReverseDNSLookupIs(domain: String) =
      DNSProviderMock.getDomainFromIP(any()) returns Some(domain)

    def aSecurityServiceFor(firewallPath: Option[String] = None,
                            parser: Parser = parserMock,
                            filters: Seq[Filter] = Nil): FileSystemSecurityService =
      new FileSystemSecurityService(
        maybePathToFirewallLogFile = None,
        maybeParser = Some(parser),
        maybeDNSDomainProvider = Some(DNSProviderMock),
        maybeFilters = Some(filters),
        maybeFileReader = Some(logReader(firewallPath).getOrElse(fileReaderMock))
      )

    private def logReader(firewallPath: Option[String]): Option[FileReader] = {
      firewallPath.map(new LogFileReader(_))
    }
  }

}
