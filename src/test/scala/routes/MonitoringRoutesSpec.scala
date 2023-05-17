package routes

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import routes.MonitoringRoutes
import utils.TestUtils.bodyToString

import java.nio.charset.Charset

class MonitoringRoutesSpec extends CatsEffectSuite {
  test("Health route should report status") {

    val healthRequest = Request[IO](Method.GET, uri"/health")
    val responseIO    = MonitoringRoutes.routes.orNotFound.run(healthRequest)

    for {
      response <- responseIO
      body     <- bodyToString(response.body)
      _ = assertEquals(response.status, Status.Ok)
      _ = assertEquals(body, "healthy!")
    } yield ()
  }
}
