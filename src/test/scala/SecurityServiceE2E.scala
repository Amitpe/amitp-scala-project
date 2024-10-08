import common.FilterTypes.{EXCLUDE, INCLUDE}
import common.{FilterTypes, FirewallParser}
import filter.{IpFilter, UserFilter}

class SecurityServiceE2E extends TestBase {

  "Security Service" should {

    "Read file, parse it, filter according to provided filters and provide domain users map" in new Context {
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

    "Read the entire file" in new Context {
      val startTime = System.nanoTime()

      val securityService = aSecurityServiceFor(
        Some("src/main/resources/firewall.log"),
        parser = new FirewallParser()
      )
      givenReverseDNSLookupIs("aws.amazon.com")
      securityService.getCloudServiceUsage()

      val endTime = System.nanoTime()
      val duration = (endTime - startTime) / 1e6 // Convert nanoseconds to milliseconds

      println(s"Time taken: ${duration} MS")
    }
  }

  trait Context extends BaseContext
}