package routes

import cats.effect.{IO, Resource}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import model.{CommandResponse, MonitorResponseStatus}
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import routes.HealthRoutes
import services.CommandsService
import utils.MunitCirceComparison
import utils.Utils.bodyToString

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.nio.charset.Charset
import scala.concurrent.duration.*

class CommandsRoutesSpec extends CatsEffectSuite with MunitCirceComparison {

  private def getResponseAndSocket = {
    val socket          = new DatagramSocket(1234, InetAddress.getLocalHost)
    val socketResource  = Resource.fromAutoCloseable(IO(socket))
    val commandsService = new CommandsService(socketResource, InetAddress.getLocalHost, 1236, 100.millis)
    val commandsRoutes  = CommandsRoutes.routes(commandsService).orNotFound
    val request         = Request[IO](Method.POST, uri"/commands/rtc")
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
      _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.Timeout).asJson)
    } yield ()
  }

  test("set-rtc route should report NACK the monitoring system") {

    val responseIOAndSocket = getResponseAndSocket
    for {
      _        <- IO.blocking(responseIOAndSocket._2.send(NACKkDatagram))
      response <- responseIOAndSocket._1
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.NACK).asJson)
    } yield ()
  }

  test("set-rtc route should report ACK the monitoring system") {
    val responseIOAndSocket = getResponseAndSocket
    for {
      _        <- IO.blocking(responseIOAndSocket._2.send(ACKDatagram))
      response <- responseIOAndSocket._1
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEqualsJson(body, CommandResponse(MonitorResponseStatus.ACK).asJson)
    } yield ()
  }
}
