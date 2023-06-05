import cats.data.Kleisli
import cats.effect.*
import cats.implicits.{showInterpolator, toSemigroupKOps}
import com.comcast.ip4s.{host, port}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpApp, HttpRoutes, Request, Response}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import routes.{CommandsRoutes, HealthRoutes}
import services.CommandsService

import java.net.{DatagramSocket, InetAddress}
import scala.concurrent.duration.*

object RepeaterMonitor extends IOApp {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  private val arduinoSocketResource = Resource.fromAutoCloseable(IO(new DatagramSocket()))
  private val configIO = ConfigSource.default.load[Configuration] match {
    case Left(error)   => IO.raiseError(new RuntimeException(error.prettyPrint()))
    case Right(config) => IO.pure(config)
  }
  def run(args: List[String]): IO[ExitCode] = configIO.flatMap { conf =>
    val commandService = new CommandsService(
      arduinoSocketResource,
      InetAddress.getByName(conf.service.arduinoIp),
      conf.service.arduinoPort,
      conf.service.responseTimeout.seconds
    )
    val httpApp = makeHttpApp(commandService)

    server(httpApp).use(server =>
      logger.info(s"Server Has Started at ${server.address}") >>
        IO.never.as(ExitCode.Success)
    )
  }

  def makeHttpApp(commandService: CommandsService): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val routes = CommandsRoutes.routes(commandService) <+> HealthRoutes.routes
    routes.orNotFound
  }

  def server(httpApp: HttpApp[IO]) = {
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
