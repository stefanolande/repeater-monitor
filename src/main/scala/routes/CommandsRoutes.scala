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
import routes.model.Config.*
import routes.model.{Config, Output, Voltages}
import services.CommandsService
import _root_.model.monitor.ConfigParam.given_Encoder_ConfigParam
import routes.model.ConfigParam.given_Decoder_ConfigParam

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

      case req @ POST -> Root / "commands" / "outputs" =>
        val payloadIO = for {
          outputs <- req.as[Output]
          _       <- logger.debug(s"received outputs $outputs")
          res     <- commandsService.setOutputs(outputs.outputNumber, outputs.status)
        } yield res.toResponse.asJson
        Ok(payloadIO)

      case req @ POST -> Root / "commands" / "config" =>
        val payloadIO = for {
          config <- req.as[Config]
          _      <- logger.debug(s"received config $config")
          res    <- commandsService.setConfig(config.param.toModel, config.value)
        } yield res.toResponse.asJson
        Ok(payloadIO)
    }

}
