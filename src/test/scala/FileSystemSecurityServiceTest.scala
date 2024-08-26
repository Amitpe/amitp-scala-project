import TestFixtures.{aLogEntry, aRandomIP, aRandomString}
import common.FilterTypes.{EXCLUDE, INCLUDE}
import common.{FilterTypes, FirewallParser}
import filter.{IpFilter, UserFilter}

class FileSystemSecurityServiceTest extends TestBase {

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
      securityService.getCloudServiceUsage()
      securityService.getCloudServiceUsage()

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

  trait Context extends BaseContext

}

