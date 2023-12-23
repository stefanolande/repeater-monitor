package socket

import cats.effect.{IO, Resource}
import model.controller.Commands.Command
import model.controller.Outcome.Timeout
import model.controller.Outcome

import java.net.{DatagramPacket, DatagramSocket, InetAddress, SocketTimeoutException}
import scala.concurrent.duration.FiniteDuration

class SocketClient(socketResource: Resource[IO, DatagramSocket], arduinoAddress: InetAddress, arduinoPort: Int, timeout: FiniteDuration) {

  private def handleResponse(arduinoSocket: DatagramSocket) = {
    val responsePacket = new DatagramPacket(new Array[Byte](128), 10)
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
}

object SocketClient {
  def make(arduinoAddress: InetAddress, arduinoPort: Int, timeout: FiniteDuration): SocketClient = {
    val socketR = Resource.fromAutoCloseable(IO(new DatagramSocket()))
    new SocketClient(socketR, arduinoAddress, arduinoPort, timeout)
  }
}
