package routes

import cats.effect.{IO, Resource}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import model.{CommandResponse, MonitorResponseStatus}
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Response, Status}
import routes.HealthRoutes
import services.CommandsService
import utils.MunitCirceComparison
import utils.Utils.bodyToString

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.nio.charset.Charset
import scala.concurrent.duration.*

class CommandsRoutesSpec extends CatsEffectSuite with MunitCirceComparison {

  private def env(f: (IO[Response[IO]], DatagramSocket) => IO[Unit]) = {
    val socket          = new DatagramSocket(1234, InetAddress.getLocalHost)
    val socketResource  = Resource.fromAutoCloseable(IO(socket))
    val commandsService = new CommandsService(socketResource, InetAddress.getLocalHost, 1236, 100.millis)
    val commandsRoutes  = CommandsRoutes.routes(commandsService).orNotFound
    val request         = Request[IO](Method.POST, uri"/commands/rtc")
    f(commandsRoutes.run(request), socket)
  }

  private val NACKkDatagram = {
    val bytes = Array('N'.byteValue())
    new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)
  }

  private val ACKDatagram = {
    val bytes = Array('A'.byteValue())
    new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)
  }

  test("set-rtc route should report timeout reaching the monitoring system") {
    env { case (responseIO, _) =>
      for {
        response <- responseIO
        body     <- bodyToString(response.body)
        _ = assertEquals(response.status, Status.Ok)
        _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.Timeout).asJson)
      } yield ()
    }
  }

  test("set-rtc route should report NACK the monitoring system") {
    env { case (responseIO, socket) =>
      for {
        _        <- IO.blocking(socket.send(NACKkDatagram))
        response <- responseIO
        body     <- bodyToString(response.body)
        _ = assertEquals(response.status, Status.Ok)
        _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.NACK).asJson)
      } yield ()
    }
  }

  test("set-rtc route should report ACK the monitoring system") {
    env { case (responseIO, socket) =>
      for {
        _        <- IO.blocking(socket.send(ACKDatagram))
        response <- responseIO
        body     <- bodyToString(response.body)
        _ = assertEquals(response.status, Status.Ok)
        _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.ACK).asJson)
      } yield ()
    }
  }
}
