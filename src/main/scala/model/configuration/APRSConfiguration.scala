package model.configuration

import com.comcast.ip4s.{Hostname, Port}
import pureconfig._
import pureconfig.generic.derivation.default._

implicit val portReader: ConfigReader[Port]     = ConfigReader[Int].map(Port.fromInt(_).get)
implicit val hostReader: ConfigReader[Hostname] = ConfigReader[String].map(Hostname.fromString(_).get)

case class APRSConfiguration(connectionCallsign: String, hostname: Hostname, port: Port, stations: List[Station]) derives ConfigReader
