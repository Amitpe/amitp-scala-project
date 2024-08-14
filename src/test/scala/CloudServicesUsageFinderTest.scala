import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class CloudServicesUsageDetectorTest extends SpecificationWithJUnit {

  "CloudServicesUsageFinder" should {

    "provide cloud service usages for an OUTGOING record" in new Context {
      val cloudServiceUsageFinder = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_1.log")
      cloudServiceUsageFinder.findCloudServicesUsages() mustEqual Map("Dropbox" -> Set("11.11.11.84"))
    }

    "provide cloud service usages for an INGOING record" in new Context {
      val cloudServiceUsageFinder = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_2.log")
      cloudServiceUsageFinder.findCloudServicesUsages() mustEqual Map("Dropbox" -> Set("192.150.249.87"))
    }

    "provide cloud service usages for both OUTGOING and INGOING record" in new Context {
      val cloudServiceUsageFinder = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_3.log")
      cloudServiceUsageFinder.findCloudServicesUsages() mustEqual Map("AWS" -> Set("11.11.11.84"), "Dropbox" -> Set("192.150.249.87"))
    }

  }

}

trait Context extends Scope {
}
