package model.configuration

case class MonitorConfiguration(port: Int, ip: String, responseTimeout: Int, stationName: String, telemetryInterval: Int)
