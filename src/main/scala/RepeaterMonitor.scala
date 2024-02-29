import cats.data.Kleisli
import cats.effect.*
import cats.implicits.toSemigroupKOps
import clients.influx.InfluxClient
import clients.monitor.RepeaterMonitorClient
import com.comcast.ip4s.{host, port}
import model.configuration.Configuration
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.{HttpApp, Response}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import routes.{APRSRoutes, CommandsRoutes, HealthRoutes}
import services.{APRSHistoryService, APRSService, CommandsService, MonitoringService}

import java.net.InetAddress
import java.security.Security
import scala.concurrent.duration.*

object RepeaterMonitor extends IOApp {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  private val configIO = ConfigSource.default.load[Configuration] match {
    case Left(error)   => IO.raiseError(new RuntimeException(error.prettyPrint()))
    case Right(config) => IO.pure(config)
  }
  def run(args: List[String]): IO[ExitCode] = configIO.flatMap { conf =>
    val resources =
      for {
        influxClient <- InfluxClient.make(
          conf.influx.host,
          conf.influx.port,
          conf.influx.token,
          conf.influx.org,
          conf.influx.bucket,
          conf.monitor.stationName
        )
        socketClient <- RepeaterMonitorClient.make(InetAddress.getByName(conf.monitor.ip), conf.monitor.port, conf.monitor.responseTimeout.seconds)
        commandsService = new CommandsService(socketClient)
        aprsHistoryService <- APRSHistoryService.make(conf.aprs.stations, influxClient)
        httpApp           = makeHttpApp(commandsService, aprsHistoryService)
        monitoringService = MonitoringService(conf.monitor.telemetryInterval.minutes, socketClient, influxClient)
        server <- server(httpApp)
      } yield (influxClient, monitoringService, server)

    resources.use { case (influxService, monitoringService, server) =>
      Security.setProperty("networkaddress.cache.ttl", "0")
      APRSService.run(conf.aprs, influxService) &>
      monitoringService.monitor &>
      logger.info(s"Server Has Started at ${server.address}") >>
      IO.never.as(ExitCode.Success)
    }
  }

  private def makeHttpApp(commandService: CommandsService, aprsHistoryService: APRSHistoryService): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}

    val routes = CommandsRoutes.routes(commandService) <+> HealthRoutes.routes <+> APRSRoutes.routes(aprsHistoryService)
    routes.orNotFound
  }

  private def server(httpApp: HttpApp[IO]) = {
    val host = host"0.0.0.0"
    val port = port"8080"

    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port)
      .withHttpApp(CORS.policy.withAllowOriginAll.apply(httpApp))
      .build
  }
}
