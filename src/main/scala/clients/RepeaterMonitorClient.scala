package clients

import cats.effect.{IO, Resource}
import model.controller.Commands.Command
import model.controller.Outcome.Timeout
import model.controller.{Commands, Outcome}

import java.net.{DatagramPacket, DatagramSocket, InetAddress, SocketTimeoutException}
import scala.concurrent.duration.FiniteDuration

class RepeaterMonitorClient(
    socketResource: Resource[IO, DatagramSocket],
    perpetualSocket: DatagramSocket,
    arduinoAddress: InetAddress,
    arduinoPort: Int,
    timeout: FiniteDuration
) {

  private def handleResponse(arduinoSocket: DatagramSocket) = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 128)
    for {
      _        <- IO(arduinoSocket.setSoTimeout(timeout.toMillis.asInstanceOf[Int]))
      maybeRes <- IO.blocking(arduinoSocket.receive(responsePacket)).attempt
      res <- maybeRes match {
        case Left(_: SocketTimeoutException) => IO.pure(Timeout)
        case Left(e)                         => IO.raiseError(e)
        case Right(_)                        => IO(Outcome.fromBytes(responsePacket.getData.array))
      }
      _ <- IO(arduinoSocket.setSoTimeout(0))
    } yield res
  }

  def send(command: Command): IO[Outcome] =
    socketResource.use { arduinoSocket =>
      val buf    = command.asBytes
      val packet = new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)
      IO.blocking(arduinoSocket.send(packet)) >> handleResponse(arduinoSocket)
    }

  // telemetry commands are expected to be sent often, so we always use the same socket to optimise
  def send(command: Commands.Telemetry): IO[Outcome] = {
    val buf    = command.asBytes
    val packet = new DatagramPacket(buf, buf.length, arduinoAddress, arduinoPort)
    IO.blocking(perpetualSocket.send(packet)) >> handleResponse(perpetualSocket)
  }
}

object RepeaterMonitorClient {
  def make(arduinoAddress: InetAddress, arduinoPort: Int, timeout: FiniteDuration): Resource[IO, RepeaterMonitorClient] = {
    val socketR          = Resource.fromAutoCloseable(IO(new DatagramSocket()))
    val perpetualSocketR = Resource.fromAutoCloseable(IO(new DatagramSocket()))
    perpetualSocketR.flatMap { perpetualSocket =>
      Resource.pure(new RepeaterMonitorClient(socketR, perpetualSocket, arduinoAddress, arduinoPort, timeout))
    }
  }
}
