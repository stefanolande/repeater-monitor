package routes

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.model.APIResponse.toResponse
import routes.model.Voltages
import services.CommandsService

object CommandsRoutes {
  private val dsl = new Http4sDsl[IO] {}
  import dsl.*

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def routes(commandsService: CommandsService): HttpRoutes[IO] = HttpRoutes
    .of[IO] {
      case POST -> Root / "commands" / "rtc" =>
        val payloadIO = for {
          _   <- logger.debug("received set-rtc")
          res <- commandsService.setRtc()
        } yield res.toResponse.asJson
        Ok(payloadIO)

      case req @ POST -> Root / "commands" / "voltages" =>
        val payloadIO = for {
          voltages <- req.as[Voltages]
          _        <- logger.debug("received set-voltages")
          res      <- commandsService.setVoltages(voltages)
        } yield res.toResponse.asJson
        Ok(payloadIO)
    }

}
