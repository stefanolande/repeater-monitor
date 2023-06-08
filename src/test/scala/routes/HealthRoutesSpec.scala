package routes

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import routes.HealthRoutes
import utils.Utils.bodyToString

import java.nio.charset.Charset

class HealthRoutesSpec extends CatsEffectSuite {
  test("Health route should report status") {

    val healthRequest = Request[IO](Method.GET, uri"/health")
    val responseIO    = HealthRoutes.routes.orNotFound.run(healthRequest)

    for {
      response <- responseIO
      _ = assertEquals(response.status, Status.NoContent)
    } yield ()
  }
}
