package common

import api.LogEntry
import common.FirewallParser._

trait Parser {
  def parseLogLine(line: String): Option[LogEntry]
}

class FirewallParser extends Parser {

  override def parseLogLine(line: String): Option[LogEntry] = {
    val direction: String = determineDirection(line)
    val parts: Map[String, String] = splitToKeyValuePairs(line)

    val userIp = extractUserIP(direction, parts)
    val cloudIp = extractCloudIP(direction, parts)

    // Extract optional fields
    val domain = parts.get(DOMAIN)
    val user = parts.get(USER)

    buildLogEntry(userIp, cloudIp, domain, user)
  }

  private def determineDirection(line: String) = {
    if (line.contains(INBOUND)) INBOUND
    else if (line.contains(OUTBOUND)) OUTBOUND
    else ""
  }

  private def splitToKeyValuePairs(line: String): Map[String, String] = {
    line.split("\\s+").collect {
      case part if part.contains("=") => part.split("=") match {
        case Array(key, value) => key -> value
        case _ => "" -> ""
      }
    }.toMap
  }

  private def extractUserIP(direction: String, parts: Map[String, String]): Option[String] = {
    direction match {
      case INBOUND => parts.get(DESTINATION)
      case OUTBOUND => parts.get(SOURCE)
      case _ => None
    }
  }

  private def extractCloudIP(direction: String, parts: Map[String, String]): Option[String] = {
    direction match {
      case INBOUND => parts.get(SOURCE)
      case OUTBOUND => parts.get(DESTINATION)
      case _ => None
    }
  }

  private def buildLogEntry(userIp: Option[String], cloudIp: Option[String], domain: Option[String], user: Option[String]) = {
    // Ensuring both userIp and cloudIp exist, otherwise return None
    (userIp, cloudIp) match {
      case (Some(userIp), Some(cloudIp)) =>
        Some(LogEntry(userIp, cloudIp, domain, user))
      case _ =>
        None
    }
  }
}

object FirewallParser {
  private final val INBOUND = "INBOUND"
  private final val OUTBOUND = "OUTG CONN"
  private final val DESTINATION = "DST"
  private final val SOURCE = "SRC"
  private final val DOMAIN = "DOMAIN"
  private final val USER = "USER"
}