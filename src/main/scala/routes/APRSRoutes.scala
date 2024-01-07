package routes

import cats.effect.IO
import clients.monitor.ConfigParam.given_Encoder_ConfigParam
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.payloads.APIResponse.toResponse
import routes.payloads.Config.*
import routes.payloads.ConfigParam.given_Decoder_ConfigParam
import routes.payloads.{Config, Output, Voltages}
import services.{APRSHistoryService, CommandsService}

object APRSRoutes {
  private val dsl = new Http4sDsl[IO] {}
  import dsl.*

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  case class APRSImport(station: String)

  def routes(APRSHistoryService: APRSHistoryService): HttpRoutes[IO] = HttpRoutes
    .of[IO] { case req @ POST -> Root / "aprs" / "import" =>
      val payloadIO = for {
        importParams <- req.as[APRSImport]
        res          <- APRSHistoryService.importHistory(importParams.station, None)
      } yield res.asJson
      Ok(payloadIO)
    }

}
