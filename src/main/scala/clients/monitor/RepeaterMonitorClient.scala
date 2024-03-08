package clients.monitor

import cats.effect.{IO, Resource}
import cats.implicits.*
import clients.monitor.Commands.*
import clients.monitor.Outcome.*
import clients.monitor.RepeaterMonitorClient
import com.comcast.ip4s.*
import fs2.io.net.{Datagram, DatagramSocket, Network}
import fs2.{Chunk, Stream, text}

import scala.concurrent.duration.FiniteDuration

class RepeaterMonitorClient(
    socketResource: Resource[IO, DatagramSocket[IO]],
    perpetualSocket: DatagramSocket[IO],
    socketAddress: SocketAddress[IpAddress],
    timeout: FiniteDuration
) {

  private def handleResponse(socket: DatagramSocket[IO]) =
    IO.race(IO.sleep(timeout), socket.read).flatMap {
      case Left(_)         => Timeout.pure[IO]
      case Right(datagram) => Outcome.fromBytes(datagram.bytes.toArray).pure[IO]
    }

  def send(command: Command): IO[Outcome] =
    socketResource.use { socket =>
      val chunk = Chunk.array(command.asBytes)
      socket.write(Datagram(socketAddress, chunk)) >> handleResponse(socket)
    }

  // telemetry commands are expected to be sent often, so we always use the same socket to optimise
  def send(command: Commands.Telemetry): IO[Outcome] = synchronized {
    val chunk = Chunk.array(command.asBytes)
    perpetualSocket.write(Datagram(socketAddress, chunk)) >> handleResponse(perpetualSocket)
  }
}

object RepeaterMonitorClient {
  def make(address: String, port: Int, timeout: FiniteDuration): Resource[IO, RepeaterMonitorClient] = {
    val socketR          = Network[IO].openDatagramSocket()
    val perpetualSocketR = Network[IO].openDatagramSocket()
    val socketAddress    = SocketAddress(Ipv4Address.fromString(address).get, Port.fromInt(port).get)
    perpetualSocketR.flatMap { perpetualSocket =>
      Resource.pure(new RepeaterMonitorClient(socketR, perpetualSocket, socketAddress, timeout))
    }
  }
}
