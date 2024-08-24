package api

case class LogEntry(userIp: String, cloudIp: String, domain: Option[String], userName: Option[String])

