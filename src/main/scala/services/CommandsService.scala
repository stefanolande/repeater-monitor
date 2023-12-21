package services

import cats.effect.{IO, Resource}
import model.Command.*
import model.MonitorResponseStatus.Timeout
import model.{MonitorResponseStatus, SetRTC, SetVoltages, Voltages}
import services.Conversions.*
import socket.SocketClient

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(socketClient: SocketClient) {

  val setRtc: IO[MonitorResponseStatus] =
    socketClient.send(SetRTC.now)

  def setVoltages(voltages: Voltages): IO[MonitorResponseStatus] =
    socketClient.send(SetVoltages(voltages))
}
