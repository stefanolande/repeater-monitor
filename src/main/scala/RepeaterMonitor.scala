import cats.data.Kleisli
import cats.effect.*
import cats.implicits.{showInterpolator, toSemigroupKOps}
import com.comcast.ip4s.{host, port}
import model.configuration.Configuration
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpApp, HttpRoutes, Request, Response}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import routes.{CommandsRoutes, HealthRoutes}
import services.{APRSService, CommandsService, InfluxService}

import java.net.{DatagramSocket, InetAddress}
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
        influxService <- InfluxService.make(conf.influx.host, conf.influx.port, conf.influx.token, conf.influx.org, conf.influx.bucket)
        commandsService = CommandsService.make(InetAddress.getByName(conf.arduinoIp), conf.arduinoPort, conf.responseTimeout.seconds)
        httpApp         = makeHttpApp(commandsService)
        server <- server(httpApp)
      } yield (influxService, server)

    resources.use { case (influxService, server) =>
      APRSService.run(conf.aprs, influxService) &>
      logger.info(s"Server Has Started at ${server.address}") >>
      IO.never.as(ExitCode.Success)
    }
  }

  private def makeHttpApp(commandService: CommandsService): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val routes = CommandsRoutes.routes(commandService) <+> HealthRoutes.routes
    routes.orNotFound
  }

  private def server(httpApp: HttpApp[IO]) = {
    val host = host"0.0.0.0"
    val port = port"8080"

    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port)
      .withHttpApp(httpApp)
      .build
  }
}
