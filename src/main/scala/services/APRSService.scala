package services

import cats.data.OptionT
import cats.effect.{IO, IOApp}
import fs2.{text, Chunk, Stream}
import fs2.io.net.{Network, Socket}
import cats.effect.MonadCancelThrow
import cats.effect.std.Console
import cats.syntax.all.*
import com.comcast.ip4s.*
import model.APRSTelemetry

import concurrent.duration.DurationInt

object APRSService extends IOApp.Simple {
  override def run: IO[Unit] =
    client.compile.drain

  def passcode(callsign: String): Int = {
    val callsignPrefix = callsign.split('-')(0).toUpperCase()
    val initialCode    = 0x73e2
    val code = callsignPrefix.zipWithIndex.foldLeft(initialCode) { case (acc, (char, i)) =>
      val shift = if (i % 2 == 0) 8 else 0
      acc ^ (char.toInt << shift)
    }
    code & 0x7fff
  }

  private def sendLogin(callsign: String, socket: Socket[IO]) =
    Stream(s"user $callsign pass ${passcode(callsign)} vers aprs-scala 0.0.1 filter p/IR0UBN\n")
      .evalTap(v => Console[IO].print(s"sending $v"))
      .through(text.utf8.encode)
      .through(socket.writes)

  case object LoginFailed extends RuntimeException("connection failed")

  def readOne(socket: Socket[IO])(f: String => IO[Unit]) = socket.reads
    .through(text.utf8.decode)
    .take(1)
    .foreach(response => f(response))

  def readForever(socket: Socket[IO])(f: String => IO[Unit]) = socket.reads
    .through(text.utf8.decode)
    .foreach(response => f(response))

  private def validateResponse(socket: Socket[IO]) =
    readOne(socket)(response =>
      if (response.startsWith("#"))
        Console[IO].print(s"Response: $response")
      else IO.raiseError(new RuntimeException(s"Invalid server response: $response"))
    )

  private def validateLogin(socket: Socket[IO]) =
    readOne(socket)(response =>
      if (!response.contains("unverified"))
        Console[IO].print(s"Login OK - $response")
      else IO.raiseError(LoginFailed)
    )

  val host = host"rotate.aprs.net"
//  val host = host"localhost"

  def client: Stream[IO, Unit] =
    Stream
      .resource(Network[IO].client(SocketAddress(host, port"14580")))
      .flatMap { socket =>
        validateResponse(socket)
        ++ sendLogin("IS0EIR", socket)
        ++ validateLogin(socket)
        ++ readForever(socket) { message =>
          APRSTelemetry.parse(message) match
            case Some(t) => Console[IO].println(s"Got telemetry: $t")
            case None    => Console[IO].print(s"Got message: $message")
        }
      }
      .handleErrorWith {
        case LoginFailed => Stream.eval(Console[IO].println(s"Login failed, exiting"))
        case e =>
          Stream.eval(
            Console[IO].println(s"Error ${e.getMessage}, waiting") >>
              IO.sleep(10.seconds)
          ) >> client
      }
}
