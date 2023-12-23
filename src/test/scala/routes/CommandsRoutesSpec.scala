package routes

import _root_.model.controller.Outcome
import _root_.model.controller.Responses.*
import cats.effect.{IO, Resource}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Response, Status}
import routes.HealthRoutes
import routes.model.APIResponse
import services.CommandsService
import socket.SocketClient
import utils.Conversions.asBytes
import utils.Utils.bodyToString
import utils.{Conversions, MunitCirceComparison}

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.nio.charset.Charset
import scala.concurrent.duration.*

class CommandsRoutesSpec extends CatsEffectSuite with MunitCirceComparison {

  private def env(f: (IO[Response[IO]], DatagramSocket) => IO[Unit]) = {
    val socket          = new DatagramSocket(1234, InetAddress.getLocalHost)
    val socketResource  = Resource.fromAutoCloseable(IO(socket))
    val socketClient    = new SocketClient(socketResource, InetAddress.getLocalHost, 1236, 100.millis)
    val commandsService = new CommandsService(socketClient)
    val commandsRoutes  = CommandsRoutes.routes(commandsService).orNotFound
    val request         = Request[IO](Method.POST, uri"/commands/rtc")
    f(commandsRoutes.run(request), socket)
  }

  private val NACKkDatagram = {
    val bytes = Array('N'.byteValue())
    new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)
  }

  private def ackDatagram(code: Byte, payload: Array[Byte]) = {
    val bytes = Array('A'.byteValue(), code) ++ payload
    new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)
  }

  test("set-rtc route should report timeout reaching the monitoring system") {
    env { case (responseIO, _) =>
      for {
        response <- responseIO
        body     <- bodyToString(response.body)
        _ = assertEquals(response.status, Status.Ok)
        _ = assertEqualsJson(body, APIResponse(Outcome.Timeout).asJson)
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
        _ = assertEqualsJson(body, APIResponse(Outcome.NACK).asJson)
      } yield ()
    }
  }

  test("set-rtc route should report ACK the monitoring system") {
    env { case (responseIO, socket) =>
      for {
        _        <- IO.blocking(socket.send(ackDatagram('R', 1234.asBytes)))
        response <- responseIO
        body     <- bodyToString(response.body)
        _ = assertEquals(response.status, Status.Ok)
        _ = assertEqualsJson(body, APIResponse(Outcome.ACK(RTC(1234))).asJson)
      } yield ()
    }
  }
}
