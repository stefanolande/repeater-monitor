package routes

import cats.effect.{IO, Resource}
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

  private def getResponseAndSocket = {
    val socket          = new DatagramSocket(1234, InetAddress.getLocalHost)
    val socketResource  = Resource.fromAutoCloseable(IO(socket))
    val commandsService = new CommandsService(socketResource, InetAddress.getLocalHost, 1236, 100.millis)
    val commandsRoutes  = CommandsRoutes.routes(commandsService).orNotFound
    val request         = Request[IO](Method.POST, uri"/commands/set-rtc")
    (commandsRoutes.run(request), socket)
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
    for {
      response <- getResponseAndSocket._1
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "no remote response")
    } yield ()
  }

  test("set-rtc route should report NACK the monitoring system") {

    val responseIOAndSocket = getResponseAndSocket
    for {
      _        <- IO.blocking(responseIOAndSocket._2.send(NACKkDatagram))
      response <- responseIOAndSocket._1
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "not executed")
    } yield ()
  }

  test("set-rtc route should report ACK the monitoring system") {
    val responseIOAndSocket = getResponseAndSocket
    for {
      _        <- IO.blocking(responseIOAndSocket._2.send(ACKDatagram))
      response <- responseIOAndSocket._1
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "executed")
    } yield ()
  }
}
