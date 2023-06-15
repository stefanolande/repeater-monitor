package model.configuration

import pureconfig._
import pureconfig.generic.derivation.default._

case class Station(callsign: String, panelsIndex: Int, batteryIndex: Int) derives ConfigReader
