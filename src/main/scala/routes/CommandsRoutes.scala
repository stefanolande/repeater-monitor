package routes

import cats.effect.IO
import model.Voltages
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.CommandsService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityDecoder._

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
        for {
          voltages <- req.as[Voltages]
          _        <- logger.debug("received set-voltages")
          resOrErr <- commandsService.setVoltages(voltages).attempt
          res      <- toResponseCode(resOrErr)
        } yield res
    }

  private def toResponseCode(status: Either[Throwable, Boolean]) = status match {
    case Right(true)                     => Ok("executed")
    case Right(false)                    => Ok("not executed")
    case Left(_: SocketTimeoutException) => Ok("no remote response")
    case Left(exception)                 => InternalServerError(exception.getMessage)
  }

}
