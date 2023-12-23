package model

import cats.data.NonEmptyList
import cats.implicits.*
import model.aprs.APRSTelemetry
import munit.FunSuite

class APRSTelemetryTest extends FunSuite {

  test("parse a correct message") {
    val msg = "IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#147,12.59,0.36,5.10,0.00,26.50,11110000\n"
    assertEquals(
      APRSTelemetry.parse(msg),
      APRSTelemetry("IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS", 147, NonEmptyList.of(12.59, 0.36, 5.10, 0.00, 26.50), "11110000").some
    )
  }

  test("return None for a malformed message") {
    val msg = "IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#"
    assertEquals(APRSTelemetry.parse(msg), None)
  }

}
