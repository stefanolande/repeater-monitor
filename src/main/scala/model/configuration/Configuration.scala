package model.configuration

import model.configuration.APRSConfiguration
import pureconfig._
import pureconfig.generic.derivation.default._

case class Configuration(
    monitor: MonitorConfiguration,
    aprs: APRSConfiguration,
    influx: InfluxConfiguration
) derives ConfigReader
