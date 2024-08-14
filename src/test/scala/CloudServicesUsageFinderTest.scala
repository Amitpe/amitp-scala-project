import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class CloudServicesUsageDetectorTest extends SpecificationWithJUnit {

  "CloudServicesUsageFinder" should {

    "return cloud service usage for an OUTGOING record" in new Context {
      val cloudServiceUsageFinder = new CloudServicesUsageFinder("src/test/resources/firewall_test_file_1.log")
      cloudServiceUsageFinder.findCloudServicesUsages() mustEqual Map("Dropbox" -> Set("11.11.11.84"))
    }

  }

}

trait Context extends Scope {
}
