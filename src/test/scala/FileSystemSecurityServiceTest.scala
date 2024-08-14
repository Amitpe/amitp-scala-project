import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class FileSystemSecurityServiceTest extends SpecificationWithJUnit {

  "FileSystemSecurityService" should {

    "provide cloud service usages for an OUTGOING record" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_1.log")
      securityService.findCloudServicesUsages() mustEqual Map("Dropbox" -> Set("11.11.11.84"))
    }

    "provide cloud service usages for an INGOING record" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_2.log")
      securityService.findCloudServicesUsages() mustEqual Map("Dropbox" -> Set("192.150.249.87"))
    }

    "provide cloud service usages for both OUTGOING and INGOING record" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_3.log")
      securityService.findCloudServicesUsages() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "ignore entries that are neither OUTGOING nor INGOING" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_4.log")
      securityService.findCloudServicesUsages() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "ignore entries that has a domain which is not found in the provided CSV" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_5.log")
      securityService.findCloudServicesUsages() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

    "provide DISTINCT IPs" in new Context {
      val securityService = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_6.log")
      securityService.findCloudServicesUsages() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

  }

}

trait Context extends Scope {
}
