package services

import cats.effect.IO
import munit.{CatsEffectSuite, FunSuite}
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status}
import routes.HealthRoutes
import utils.TestUtils.bodyToString

import java.nio.charset.Charset

class CommandsServiceSpec extends FunSuite {
  test("timestamp") {

    val timestamp = 1685913409L

//    val bytes = CommandsService.timeToRTCCommandPacket(timestamp)
//    println(bytes)

    'C'.toByte
  }
}
