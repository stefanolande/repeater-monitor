import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.implicits.showInterpolator
import com.comcast.ip4s.{host, port}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import routes.MonitoringRoutes

object RepeaterMonitor extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    server.use(server =>
      IO.delay(println(s"Server Has Started at ${server.address}")) >>
        IO.never.as(ExitCode.Success)
    )

  private val httpApp = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    val routes = MonitoringRoutes.routes
    routes.orNotFound
  }

  private val server = {
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
