package routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.CommandsService

import java.net.SocketTimeoutException

object CommandsRoutes {
  private val dsl = new Http4sDsl[IO] {}
  import dsl.*

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def routes(commandsService: CommandsService): HttpRoutes[IO] = HttpRoutes
    .of[IO] {
      case POST -> Root / "commands" / "set-rtc" =>
        logger.debug("received set-rtc") >>
        commandsService.setRtc.attempt.flatMap(toResponseCode)

      case req @ POST -> Root / "commands" / "set-voltages" =>
        logger.debug("received set-voltages") >>
        commandsService.setVoltages.attempt.flatMap(toResponseCode)
    }

  private def toResponseCode(status: Either[Throwable, Boolean]) = status match {
    case Right(true)                     => Ok("executed")
    case Right(false)                    => Ok("not executed")
    case Left(_: SocketTimeoutException) => Ok("no remote response")
    case Left(exception)                 => InternalServerError(exception.getMessage)
  }

}
