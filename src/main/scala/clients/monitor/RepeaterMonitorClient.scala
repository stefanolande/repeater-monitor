package clients.monitor

import cats.effect.{IO, Resource}
import clients.monitor.Commands.*
import clients.monitor.Outcome.*
import clients.monitor.RepeaterMonitorClient

import java.net.{DatagramPacket, DatagramSocket, InetAddress, SocketTimeoutException}
import scala.concurrent.duration.FiniteDuration

class RepeaterMonitorClient(
    socketResource: Resource[IO, DatagramSocket],
    perpetualSocket: DatagramSocket,
    address: InetAddress,
    port: Int,
    timeout: FiniteDuration
) {

  private def handleResponse(socket: DatagramSocket) = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 128)
    for {
      _        <- IO(socket.setSoTimeout(timeout.toMillis.asInstanceOf[Int]))
      maybeRes <- IO.blocking(socket.receive(responsePacket)).attempt
      res <- maybeRes match {
        case Left(_: SocketTimeoutException) => IO.pure(Timeout)
        case Left(e)                         => IO.raiseError(e)
        case Right(_)                        => IO(Outcome.fromBytes(responsePacket.getData.array.slice(0, responsePacket.getLength)))
      }
      _ <- IO(socket.setSoTimeout(0))
    } yield res
  }

  def send(command: Command): IO[Outcome] =
    socketResource.use { socket =>
      val buf    = command.asBytes
      val packet = new DatagramPacket(buf, buf.length, address, port)
      IO.blocking(socket.send(packet)) >> handleResponse(socket)
    }

  // telemetry commands are expected to be sent often, so we always use the same socket to optimise
  def send(command: Commands.Telemetry): IO[Outcome] = synchronized {
    val buf    = command.asBytes
    val packet = new DatagramPacket(buf, buf.length, address, port)
    IO.blocking(perpetualSocket.send(packet)) >> handleResponse(perpetualSocket)
  }
}

object RepeaterMonitorClient {
  def make(address: InetAddress, port: Int, timeout: FiniteDuration): Resource[IO, RepeaterMonitorClient] = {
    val socketR          = Resource.fromAutoCloseable(IO(new DatagramSocket()))
    val perpetualSocketR = Resource.fromAutoCloseable(IO(new DatagramSocket()))
    perpetualSocketR.flatMap { perpetualSocket =>
      Resource.pure(new RepeaterMonitorClient(socketR, perpetualSocket, address, port, timeout))
    }
  }
}
