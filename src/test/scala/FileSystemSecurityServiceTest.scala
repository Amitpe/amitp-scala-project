import TestFixtures.{aLogEntry, aRandomIP, aRandomString}
import api.LogEntry
import common.FilterTypes.{EXCLUDE, INCLUDE}
import common.{Filter, FilterTypes, FirewallParser, Parser}
import filter.{IpFilter, UserFilter}
import io.{DNSDomainProvider, FileReader, LogFileReader}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import org.specs2.mock.Mockito.{any, mock, theStubbed}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class FileSystemSecurityServiceTest extends SpecificationWithJUnit with Mockito {

  "FileSystemSecurityService" should {

    // Following tests are for the first checkpoint
    "provide cloud service usages for a single domain with a single IP" in new Context {
      val logEntry = givenLogEntry(aLogEntry(domain = Some("www.dropbox.com")))
      val securityService = aSecurityServiceFor()
      securityService.getCloudServiceUsage() mustEqual Map("Dropbox" -> Set(logEntry.userIp))
    }

    "provide cloud service usages for multiple domains and IPs" in new Context {
      val dropboxFirstIp = "11.11.11.84"
      val dropboxSecondIp = "11.11.11.85"

      val awsFirstIp = "11.11.11.86"
      val awsSecondIp = "11.11.11.87"

      givenLogEntries(
        aLogEntry(userIp = dropboxFirstIp, domain = Some("www.dropbox.com")),
        aLogEntry(userIp = dropboxSecondIp, domain = Some("www.dropbox.com")),
        aLogEntry(userIp = awsFirstIp, domain = Some("aws.amazon.com")),
        aLogEntry(userIp = awsSecondIp, domain = Some("aws.amazon.com")),
      )

      val securityService = aSecurityServiceFor()

      securityService.getCloudServiceUsage() mustEqual Map(
        "Dropbox" -> Set(dropboxFirstIp, dropboxSecondIp),
        "AWS" -> Set(awsFirstIp, awsSecondIp)
      )
    }

    "ignore entries that has a domain which is not found in the provided CSV" in new Context {
      val dropboxFirstIp = "11.11.11.84"
      val dropboxSecondIp = "11.11.11.85"

      givenLogEntries(
        aLogEntry(userIp = dropboxFirstIp, domain = Some("www.dropbox.com")),
        aLogEntry(userIp = dropboxSecondIp, domain = Some("www.dropbox.com")),
        aLogEntry(userIp = aRandomIP(), domain = Some(aRandomString())),
      )

      val securityService = aSecurityServiceFor()

      securityService.getCloudServiceUsage() mustEqual Map(
        "Dropbox" -> Set(dropboxFirstIp, dropboxSecondIp)
      )
    }

    "provide DISTINCT IPs" in new Context {
      val dropboxFirstIp = "11.11.11.84"
      val dropboxSecondIp = "11.11.11.84"

      givenLogEntries(
        aLogEntry(userIp = dropboxFirstIp, domain = Some("www.dropbox.com")),
        aLogEntry(userIp = dropboxSecondIp, domain = Some("www.dropbox.com")),
      )

      val securityService = aSecurityServiceFor()

      securityService.getCloudServiceUsage() mustEqual Map(
        "Dropbox" -> Set(dropboxFirstIp)
      )
    }

    // Following tests are for the second checkpoint
    "find cloud services for entries that has no domain, by performing DNS reverse lookup" in new Context {
      val securityService = aSecurityServiceFor()
      givenReverseDNSLookupIs("aws.amazon.com")
      val logEntry = givenLogEntry(aLogEntry(domain = None))
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set(logEntry.userIp))
    }

    // TODO - those tests still read log file, we should refactor to use the mocks as the tests above
    // TODO - still need to write an E2E test class that will read a log file
    "cache results from reverse DNS lookup" in new Context {
      val securityService = aSecurityServiceFor(Some("src/test/resources/firewall_test_file_7.log"), parser = new FirewallParser())
      givenReverseDNSLookupIs("aws.amazon.com")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))

      there was one(DNSProviderMock).getDomainFromIP(any())
    }

    // Following tests are for the third checkpoint
    "filter by IP - include" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(IpFilter("11.11.11.84", filterType = FilterTypes.INCLUDE))
      )

      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
    }

    "filter by IP - include with range" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(IpFilter("11.11.11.84", range = Some(32), FilterTypes.INCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
    }

    "filter by IP - exclude" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(IpFilter("11.11.11.84", filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.85", "11.11.11.86"))
    }

    "filter by IP - exclude with range" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(IpFilter("11.11.11.84", range = Some(32), filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.85", "11.11.11.86"))
    }

    "filter by user - include" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(new UserFilter("^ra.*", filterType = FilterTypes.INCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86"))
    }

    "filter by user - exclude" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_8.log"),
        parser = new FirewallParser(),
        filters = Seq(new UserFilter("acme\\.com$", filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86"))
    }

    "filter by user exclusion should not effect log entries that has no user name" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_9.log"),
        parser = new FirewallParser(),
        filters = Seq(new UserFilter("acme\\.com$", filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86", "11.11.11.87"))
    }

    "filter multiple kind of filters" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_10.log"),
        parser = new FirewallParser(),
        filters = Seq(
          new UserFilter("acme\\.com$", filterType = FilterTypes.EXCLUDE),
          IpFilter("11.11.11.87", filterType = EXCLUDE),
          IpFilter("11.11.11.89", filterType = INCLUDE),
        )
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.89"))
    }

    "a single filter by IP should accept multiple IPs" in new Context {
      val securityService = aSecurityServiceFor(
        Some("src/test/resources/firewall_test_file_10.log"),
        parser = new FirewallParser(),
        filters = Seq(
          new IpFilter(Seq("11.11.11.87", "11.11.11.89"), filterType = INCLUDE),
        )
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.87", "11.11.11.89"))
    }

  }

}

trait Context extends Scope {
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
