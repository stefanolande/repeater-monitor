package model.configuration

import model.configuration.APRSConfiguration
import pureconfig._
import pureconfig.generic.derivation.default._

case class Configuration(
    arduinoPort: Int,
    arduinoIp: String,
    responseTimeout: Int,
    aprs: APRSConfiguration,
    influx: InfluxConfiguration
) derives ConfigReader
