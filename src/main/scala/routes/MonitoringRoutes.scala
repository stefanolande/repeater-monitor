package routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object MonitoringRoutes {
  private val dsl = new Http4sDsl[IO] {}
  import dsl.*

  val routes: HttpRoutes[IO] = HttpRoutes
    .of[IO] { case GET -> Root / "health" =>
      Ok("healthy!")
    }

}
