package routes

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import routes.HealthRoutes
import services.CommandsService
import utils.TestUtils.bodyToString

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.nio.charset.Charset
import scala.concurrent.duration.*

class CommandsRoutesSpec extends CatsEffectSuite {

  private val socket          = new DatagramSocket(1234, InetAddress.getLocalHost)
  private val commandsService = new CommandsService(InetAddress.getLocalHost, 1236, socket, 100.millis)
  private val commandsRoutes  = CommandsRoutes.routes(commandsService).orNotFound
  private val request         = Request[IO](Method.POST, uri"/commands/set-rtc")

  test("set-rtc route should report timeout reaching the monitoring system") {
    val responseIO = commandsRoutes.run(request)

    for {
      response <- responseIO
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "no remote response")
    } yield ()
  }

  test("set-rtc route should report NACK the monitoring system") {
    val responseIO = commandsRoutes.run(request)

    val bytes  = Array('N'.byteValue())
    val packet = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)

    for {
      _        <- IO.blocking(socket.send(packet))
      response <- responseIO
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "not executed")
    } yield ()
  }

  test("set-rtc route should report ACK the monitoring system") {
    val responseIO = commandsRoutes.run(request)

    val bytes  = Array('A'.byteValue())
    val packet = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost, 1234)

    for {
      _        <- IO.blocking(socket.send(packet))
      response <- responseIO
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "executed")
    } yield ()
  }
}
