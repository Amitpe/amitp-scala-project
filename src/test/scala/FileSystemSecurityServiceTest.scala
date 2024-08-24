import common.FilterTypes.{EXCLUDE, INCLUDE}
import common.{Filter, FilterTypes}
import filter.{IpFilter, UserFilter}
import io.DNSDomainProvider
import org.specs2.mock.Mockito
import org.specs2.mock.Mockito.{any, mock, theStubbed}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class FileSystemSecurityServiceTest extends SpecificationWithJUnit with Mockito {

  "FileSystemSecurityService" should {

    // Following tests are for the first checkpoint
    "provide cloud service usages for an OUTGOING record" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_1.log")
      securityService.getCloudServiceUsage() mustEqual Map("Dropbox" -> Set("11.11.11.84"))
    }

    "provide cloud service usages for an INGOING record" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_2.log")
      securityService.getCloudServiceUsage() mustEqual Map("Dropbox" -> Set("192.150.249.87"))
    }

    "provide cloud service usages for both OUTGOING and INGOING record" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_3.log")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "ignore entries that are neither OUTGOING nor INGOING" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_4.log")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "ignore entries that has a domain which is not found in the provided CSV" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_5.log")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "provide DISTINCT IPs" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_6.log")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    // Following tests are for the second checkpoint
    "find cloud services for entries that has no domain" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_7.log")
      givenReverseDNSLookupIs("aws.amazon.com")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
    }

    "cache results from reverse DNS lookup" in new Context {
      val securityService = aSecurityServiceFor("src/test/resources/firewall_test_file_7.log")
      givenReverseDNSLookupIs("aws.amazon.com")
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))

      there was one(DNSProviderSpy).getDomainFromIP(any())
    }

    // Following tests are for the third checkpoint
    "filter by IP - include" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(IpFilter("11.11.11.84" ,filterType = FilterTypes.INCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
    }

    "filter by IP - include with range" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(IpFilter("11.11.11.84", range = Some(32) ,FilterTypes.INCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.84"))
    }

    "filter by IP - exclude" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(IpFilter("11.11.11.84" ,filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.85", "11.11.11.86"))
    }

    "filter by IP - exclude with range" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(IpFilter("11.11.11.84" , range = Some(32), filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.85", "11.11.11.86"))
    }

    "filter by user - include" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(new UserFilter("^ra.*" , filterType = FilterTypes.INCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86"))
    }

    "filter by user - exclude" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_8.log",
        filters = Seq(new UserFilter("acme\\.com$", filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86"))
    }

    "filter by user exclusion should not effect log entries that has no user name" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_9.log",
        filters = Seq(new UserFilter("acme\\.com$", filterType = FilterTypes.EXCLUDE))
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.86", "11.11.11.87"))
    }

    "filter multiple kind of filters" in new Context {
      val securityService = aSecurityServiceFor(
        "src/test/resources/firewall_test_file_10.log",
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
        "src/test/resources/firewall_test_file_10.log",
        filters = Seq(
          new IpFilter(Seq("11.11.11.87", "11.11.11.89"), filterType = INCLUDE),
        )
      )
      securityService.getCloudServiceUsage() mustEqual Map("AWS" -> Set("11.11.11.87", "11.11.11.89"))
    }

//    "filter by IP should not accept invalid filters" in new Context {
//
//    }

  }

}

trait Context extends Scope {

  // Mocking the reverse DNS lookup since it's not stable - we want the test result to the consistent
  val DNSProviderSpy = mock[DNSDomainProvider]

  def givenReverseDNSLookupIs(domain: String) =
    DNSProviderSpy.getDomainFromIP(any()) returns Some(domain)

  def aSecurityServiceFor(firewallPath: String,
                          filters: Seq[Filter] = Nil): FileSystemSecurityService =
    new FileSystemSecurityService(
      maybePathToFirewallLogFile = Some(firewallPath),
      maybeDNSDomainProvider = Some(DNSProviderSpy),
      maybeFilters = Some(filters)
    )
}
