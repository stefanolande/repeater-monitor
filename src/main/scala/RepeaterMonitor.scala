import cats.data.Kleisli
import cats.effect.*
import cats.implicits.{showInterpolator, toSemigroupKOps}
import com.comcast.ip4s.{host, port}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpApp, HttpRoutes, Request, Response}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.{CommandsRoutes, HealthRoutes}
import services.CommandsService

import java.net.{DatagramSocket, InetAddress}
import scala.concurrent.duration._

object RepeaterMonitor extends IOApp {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  private val arduinoSocketResource = Resource.fromAutoCloseable(IO(new DatagramSocket()))

  def run(args: List[String]): IO[ExitCode] =
    arduinoSocketResource.use { arduinoSocket =>

      val commandService = new CommandsService(InetAddress.getByName("172.29.10.66"), 8888, arduinoSocket, 5.seconds)
      val httpApp        = makeHttpApp(commandService)

      server(httpApp).use(server =>
        logger.info(s"bound to port ${arduinoSocket.getLocalPort}") >>
          IO.delay(println(s"Server Has Started at ${server.address}")) >>
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
