package common

import api.LogEntry

import scala.util.matching.Regex

trait Parser {
  def parseLogLine(line: String): Option[LogEntry]
}

class FirewallParser extends Parser {
  def parseLogLine(line: String): Option[LogEntry] = {
    // Ensure the line contains 'kernel'
    if (!line.contains("kernel")) return None

    // Find the part of the line starting after 'kernel'
    val kernelIndex = line.indexOf("kernel") + "kernel: ".length
    val restOfLine = line.substring(kernelIndex)

    // Find the direction (INBOUND or OUTG CONN)
    val direction = if (restOfLine.contains("INBOUND")) "INBOUND"
    else if (restOfLine.contains("OUTG CONN")) "OUTG"
    else ""

    // Split the rest of the line by spaces to extract IP and domain
    val parts = restOfLine.split(" ")

    // Extract IP based on direction
    val (userIp, cloudIp) = direction match {
      case "INBOUND" =>
        (
          parts.find(_.startsWith("DST=")).map(_.split("=")(1)),
          parts.find(_.startsWith("SRC=")).map(_.split("=")(1))
        )
      case "OUTG" =>
        (
          parts.find(_.startsWith("SRC=")).map(_.split("=")(1)),
          parts.find(_.startsWith("DST=")).map(_.split("=")(1)),
        )
      case _ => (None, None)
    }

    // Extract domain if present
    val domain = parts.find(_.startsWith("DOMAIN=")).map(_.split("=")(1))

    // Extract user if present
    val user = parts.find(_.startsWith("USER=")).map(_.split("=")(1))

    (userIp, cloudIp) match {
      case (Some(userIp), Some(cloudIp)) =>
        Some(LogEntry(userIp, cloudIp, domain, user))
      case _ =>
        None
    }
  }

}