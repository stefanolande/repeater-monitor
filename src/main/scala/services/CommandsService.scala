package services

import cats.effect.{IO, Resource}
import model.Command.*
import model.Command.getCode
import model.MonitorResponseStatus.Timeout
import model.{MonitorResponseStatus, Voltages}
import services.Conversions.*

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(socketResource: Resource[IO, DatagramSocket], arduinoAddress: InetAddress, arduinoPort: Int, timeout: FiniteDuration) {

  private def handleResponse(arduinoSocket: DatagramSocket) = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 10)
    for {
      _        <- IO(arduinoSocket.setSoTimeout(timeout.toMillis.asInstanceOf[Int]))
      maybeRes <- IO.blocking(arduinoSocket.receive(responsePacket)).attempt
      res <- maybeRes match {
        case Left(_: SocketTimeoutException) => IO.pure(Timeout)
        case Left(e)                         => IO.raiseError(e)
        case Right(_)                        => IO(MonitorResponseStatus.fromByte(responsePacket.getData.array(0)))
      }
      _ <- IO(arduinoSocket.setSoTimeout(0))
    } yield res
  }

  private def timeToRTCCommandPacket(timestamp: Long): DatagramPacket = {
    val intTimestamp: Int = (timestamp / 1000).asInstanceOf[Int]

    val buf = Array(SetRTC.getCode) ++ intTimestamp.asBytes
    new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)
  }

  val setRtc: IO[MonitorResponseStatus] =
    socketResource.use { arduinoSocket =>
      for {
        timestamp <- IO(System.currentTimeMillis)
        packet    <- IO(timeToRTCCommandPacket(timestamp))
        _         <- IO.blocking(arduinoSocket.send(packet))
        res       <- handleResponse(arduinoSocket)
      } yield res
    }
  def setVoltages(voltages: Voltages): IO[MonitorResponseStatus] =
    socketResource.use { arduinoSocket =>
      val buf    = Array(SetVoltages.getCode) ++ voltages.offVoltage.asBytes ++ voltages.onVoltage.asBytes
      val packet = new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)

      IO.blocking(arduinoSocket.send(packet)) >> handleResponse(arduinoSocket)
    }
}
