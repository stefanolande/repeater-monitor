package services

import cats.effect.{IO, Resource}
import model.Voltages
import Utils.*

import java.net.{DatagramPacket, DatagramSocket, InetAddress, Socket}
import java.nio.ByteBuffer
import concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(socketResource: Resource[IO, DatagramSocket], arduinoAddress: InetAddress, arduinoPort: Int, timeout: FiniteDuration) {

  private def handleAck(arduinoSocket: DatagramSocket) = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 10)
    for {
      _   <- IO(arduinoSocket.setSoTimeout(timeout.toMillis.asInstanceOf[Int]))
      _   <- IO.blocking(arduinoSocket.receive(responsePacket))
      _   <- IO(arduinoSocket.setSoTimeout(0))
      res <- IO(responsePacket.getData.array(0) == 'A')
    } yield res
  }

  private def timeToRTCCommandPacket(timestamp: Long): DatagramPacket = {
    val intTimestamp: Int = (timestamp / 1000).asInstanceOf[Int]

    val buf = Array(SetRTC.getCode.toByte) ++ intTimestamp.asBytes
    new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)
  }

  val setRtc: IO[Boolean] =
    socketResource.use { arduinoSocket =>
      for {
        timestamp <- IO(System.currentTimeMillis)
        packet    <- IO(timeToRTCCommandPacket(timestamp))
        _         <- IO.blocking(arduinoSocket.send(packet))
        res       <- handleAck(arduinoSocket)
      } yield res
    }
  def setVoltages(voltages: Voltages): IO[Boolean] =
    socketResource.use { arduinoSocket =>
      val buf    = Array(SetVoltages.getCode.toByte) ++ voltages.offVoltage.asBytes ++ voltages.onVoltage.asBytes
      val packet = new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)

      IO.blocking(arduinoSocket.send(packet)) >> handleAck(arduinoSocket)
    }
}

sealed trait Command(code: Char) {
  def getCode: Char = code
}
case object SetRTC extends Command('S')
case object SetVoltages extends Command('V')
