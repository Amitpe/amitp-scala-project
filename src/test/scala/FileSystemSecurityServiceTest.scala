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

  }

}

trait Context extends Scope {

  val fakeDNSDomainProvider = mock[DNSDomainProvider]

  def givenReverseDNSLookupIs(domain: String) =
    fakeDNSDomainProvider.getDomainFromIP(any()) returns Some(domain)

  def aSecurityServiceFor(firewallPath: String): FileSystemSecurityService =
    new FileSystemSecurityService(
      maybePathToFirewallLogFile = Some(firewallPath),
      maybeDNSDomainProvider = Some(fakeDNSDomainProvider)
    )
}
