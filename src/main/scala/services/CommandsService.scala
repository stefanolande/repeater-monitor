package services

import cats.effect.IO

import java.net.{DatagramPacket, DatagramSocket, InetAddress, Socket}
import java.nio.ByteBuffer
import concurrent.duration.{DurationInt, FiniteDuration}

class CommandsService(arduinoAddress: InetAddress, arduinoPort: Int, arduinoSocket: DatagramSocket, timeout: FiniteDuration) {

  private val handleAck = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 10)
    for {
      _   <- IO(arduinoSocket.setSoTimeout(timeout.toMillis.asInstanceOf[Int]))
      _   <- IO.blocking(arduinoSocket.receive(responsePacket))
      _   <- IO(arduinoSocket.setSoTimeout(0))
      res <- IO(responsePacket.getData.apply(0) == 'A')
    } yield res
  }

  def timeToRTCCommandPacket(timestamp: Long): DatagramPacket = {
    val intTimestamp: Int = (timestamp / 1000).asInstanceOf[Int]

    val buf =
      Array(SetRTC.getCode, intTimestamp & 0xff, (intTimestamp >> 8) & 0xff, (intTimestamp >> 16) & 0xff, (intTimestamp >> 24) & 0xff).map(_.toByte)
    new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)
  }

  val setRtc: IO[Boolean] =
    for {
      timestamp <- IO(System.currentTimeMillis)
      packet    <- IO(timeToRTCCommandPacket(timestamp))
      _         <- IO.blocking(arduinoSocket.send(packet))
      res       <- handleAck
    } yield res

  val setVoltages: IO[Boolean] = {
    val buf =
      Array(SetVoltages.getCode, 0x04, 0x29, 0x00, 0x32).map(_.toByte)
    val packet = new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)

    IO.blocking(arduinoSocket.send(packet)) >> handleAck
  }

}

sealed trait Command(code: Char) {
  def getCode: Char = code
}
case object SetRTC extends Command('S')
case object SetVoltages extends Command('V')
