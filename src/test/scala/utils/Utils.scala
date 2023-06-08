package utils

import cats.effect.Async
import io.circe.Json
import org.http4s.EntityBody

import java.nio.charset.Charset
import io.circe.parser.*
import munit.Assertions

object Utils {
  def bodyToString[F[_]: Async](body: EntityBody[F]): F[String] =
    body.through(fs2.text.decodeWithCharset(Charset.defaultCharset())).foldMonoid.compile.lastOrError
}

trait MunitCirceComparison extends Assertions {
  def assertEqualsJson(obtained: String, expected: Json): Unit =
    parse(obtained) match
      case Left(e)       => throw fail("json parsing failed", e)
      case Right(parsed) => assertEquals(parsed, expected)
}
