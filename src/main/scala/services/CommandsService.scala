package services

import cats.effect.{IO, Resource}
import model.*
import clients.monitor.Commands.{Command, ConfigSet, OutputSet, RTCSet}
import clients.monitor.ConfigParam.{MainVoltageOff, MainVoltageOn}
import clients.monitor.{ConfigParam, Outcome, RepeaterMonitorClient}
import clients.monitor.Outcome.Timeout
import routes.payloads.Voltages
import utils.Conversions.*

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(monitorClient: RepeaterMonitorClient) {

  def setRtc(): IO[Outcome] =
    monitorClient.send(RTCSet.now)

  def setVoltages(voltages: Voltages): IO[Outcome] =
    monitorClient.send(ConfigSet(MainVoltageOn, voltages.onVoltage))
    monitorClient.send(ConfigSet(MainVoltageOff, voltages.offVoltage))

  def setOutputs(outputNumber: Int, status: Boolean): IO[Outcome] = monitorClient.send(OutputSet(outputNumber, status))

  def setConfig(param: ConfigParam, value: Float): IO[Outcome] = monitorClient.send(ConfigSet(param, value))
}
