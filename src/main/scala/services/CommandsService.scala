package services

import cats.effect.{IO, Resource}
import model.*
import model.controller.Commands.{Command, ConfigSet, RTCSet}
import model.controller.ConfigParam.{MainVoltageOff, MainVoltageOn}
import model.controller.Outcome
import model.controller.Outcome.Timeout
import routes.model.Voltages
import utils.Conversions.*
import clients.RepeaterMonitorClient

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(socketClient: RepeaterMonitorClient) {

  def setRtc(): IO[Outcome] =
    socketClient.send(RTCSet.now)

  def setVoltages(voltages: Voltages): IO[Outcome] =
    socketClient.send(ConfigSet(MainVoltageOn, voltages.onVoltage))
    socketClient.send(ConfigSet(MainVoltageOff, voltages.offVoltage))
}
