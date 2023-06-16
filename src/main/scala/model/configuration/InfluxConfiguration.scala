package model.configuration

import com.comcast.ip4s.{Hostname, Port}
import pureconfig._
import pureconfig.generic.derivation.default._

case class InfluxConfiguration(host: Hostname, port: Port, token: String, org: String, bucket: String) derives ConfigReader
