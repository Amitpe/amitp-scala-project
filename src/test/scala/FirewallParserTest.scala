import api.LogEntry
import common.FirewallParser
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class FirewallParserTest extends SpecificationWithJUnit {

  "FirewallParser" should {

    "an INGOING recorde should map DST to userIp and SRC to cloudIp" in new Context {
      parser.parseLogLine("Feb  1 00:00:02 bridge kernel: INBOUND TCP: IN=br0 PHYSIN=eth0 OUT=br0 PHYSOUT=eth1 SRC=192.150.249.87 " +
        "DST=11.11.11.84 LEN=40 TOS=0x00 PREC=0x00 TTL=110 ID=12973 PROTO=TCP SPT=220 DPT=6129 WINDOW=16384 RES=0x00 " +
        "SYN URGP=0 USER=dave@acme.com DOMAIN=www.dropbox.com"
      ) must beSome(LogEntry(
        userIp = "11.11.11.84",
        cloudIp = "192.150.249.87",
        domain = Some("www.dropbox.com"),
        userName = Some("dave@acme.com")
      ))
    }

    "an OUTGOING recorde should map SRC to cloudIp and DST to userIp" in new Context {
      parser.parseLogLine("Feb  1 00:00:02 bridge kernel: OUTG CONN TCP: IN=br0 PHYSIN=eth0 OUT=br0 PHYSOUT=eth1 SRC=11.11.11.84 " +
        "DST=192.150.249.87 LEN=40 TOS=0x00 PREC=0x00 TTL=110 ID=12973 PROTO=TCP SPT=220 DPT=6129 WINDOW=16384 RES=0x00 " +
        "SYN URGP=0 USER=dave@acme.com DOMAIN=www.dropbox.com"
      ) must beSome(LogEntry(
        userIp = "11.11.11.84",
        cloudIp = "192.150.249.87",
        domain = Some("www.dropbox.com"),
        userName = Some("dave@acme.com")
      ))
    }

    "provide None for entries that are neither OUTGOING nor INGOING" in new Context {
      parser.parseLogLine("Feb  1 00:00:02 bridge TCP: IN=br0 PHYSIN=eth0 OUT=br0 PHYSOUT=eth1 SRC=11.11.11.84 " +
        "DST=192.150.249.87 LEN=40 TOS=0x00 PREC=0x00 TTL=110 ID=12973 PROTO=TCP SPT=220 DPT=6129 WINDOW=16384 RES=0x00 " +
        "SYN URGP=0 USER=dave@acme.com DOMAIN=www.dropbox.com"
      ) must beNone
    }

    "provide None for the optional fields when they don't exist" in new Context {
      parser.parseLogLine("Feb  1 00:00:02 bridge kernel: OUTG CONN TCP: IN=br0 PHYSIN=eth0 OUT=br0 PHYSOUT=eth1 SRC=11.11.11.84 " +
        "DST=192.150.249.87 LEN=40 TOS=0x00 PREC=0x00 TTL=110 ID=12973 PROTO=TCP SPT=220 DPT=6129 WINDOW=16384 RES=0x00 " +
        "SYN URGP=0"
      ) must beSome(LogEntry(
        userIp = "11.11.11.84",
        cloudIp = "192.150.249.87",
        domain = None,
        userName = None
      ))
    }
  }

  trait Context extends Scope {
    val parser = new FirewallParser()
  }

}

