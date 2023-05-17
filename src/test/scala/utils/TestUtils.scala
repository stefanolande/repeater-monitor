package utils

import cats.effect.Async
import org.http4s.EntityBody

import java.nio.charset.Charset

object TestUtils {
  def bodyToString[F[_]: Async](body: EntityBody[F]): F[String] =
    body.through(fs2.text.decodeWithCharset(Charset.defaultCharset())).foldMonoid.compile.lastOrError
}
