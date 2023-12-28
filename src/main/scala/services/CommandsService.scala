package services

import cats.effect.{IO, Resource}
import clients.RepeaterMonitorClient
import model.*
import model.monitor.Commands.{Command, ConfigSet, OutputSet, RTCSet}
import model.monitor.ConfigParam.{MainVoltageOff, MainVoltageOn}
import model.monitor.Outcome.Timeout
import model.monitor.{ConfigParam, Outcome}
import routes.model.Voltages
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
