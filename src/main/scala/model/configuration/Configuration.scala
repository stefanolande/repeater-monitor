package model.configuration

import model.configuration.APRSConfiguration
import pureconfig._
import pureconfig.generic.derivation.default._

case class Configuration(
    arduino: ArduinoConfiguration,
    aprs: APRSConfiguration,
    influx: InfluxConfiguration
) derives ConfigReader
