import api.LogEntry
import org.apache.commons.text.RandomStringGenerator

import scala.util.Random

object TestFixtures {

  private val generator = new RandomStringGenerator.Builder()
    .withinRange('a', 'z') // Adjust range as needed
    .build()

  def aLogEntry(userIp: String = aRandomIP(),
                cloudIp: String = aRandomIP(),
                domain: Option[String] = Some(aRandomString()),
                userName: Option[String] = Some(aRandomString())): LogEntry =
    LogEntry(
      userIp = userIp,
      cloudIp = cloudIp,
      domain = domain,
      userName = userName
    )

  def aRandomIP(): String = {
    val random = new Random()
    val octets = Array.fill(4)(random.nextInt(256)) // Generate 4 octets (0-255)
    octets.mkString(".")
  }

  def aRandomString(): String =
    generator.generate(10)

}
