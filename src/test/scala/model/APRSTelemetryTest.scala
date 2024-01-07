package model

import cats.data.NonEmptyList
import cats.implicits.*
import model.aprs.APRSTelemetry
import munit.FunSuite

import java.time.LocalDateTime

class APRSTelemetryTest extends FunSuite {

  test("parse a correct message w/o timestamp") {
    val msg = "IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#147,12.59,0.36,5.10,0.00,26.50,11110000\n"
    assertEquals(
      APRSTelemetry.parse(msg),
      APRSTelemetry("IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS", 147, NonEmptyList.of(12.59, 0.36, 5.10, 0.00, 26.50), "11110000", None).some
    )
  }

  test("parse a correct message w timestamp") {
    val msg = " 20240106111513,IR0UBN>APDW16,WIDE1-1,qAO,IS0XDA-12:T#245,11.57,0.34,5.12,12.70,15.88,11110000\n"
    assertEquals(
      APRSTelemetry.parse(msg),
      APRSTelemetry(
        "IR0UBN>APDW16,WIDE1-1,qAO,IS0XDA-12",
        245,
        NonEmptyList.of(11.57, 0.34, 5.12, 12.70, 15.88),
        "11110000",
        LocalDateTime.of(2024, 1, 6, 11, 15, 13).some
      ).some
    )
  }

  test("return None for a malformed message") {
    val msg = "IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#"
    assertEquals(APRSTelemetry.parse(msg), None)
  }

}
