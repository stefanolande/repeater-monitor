package services

import munit.FunSuite
import utils.Conversions._

class ConversionsTest extends FunSuite {

  test("Int to byte conversion") {
    val i              = 13800
    val b: Array[Byte] = Array(0, 0, 53, -24)
    assertEquals(i.asBytes.toSeq, b.toSeq)
  }

  test("Float to byte conversion") {
    val i: Float       = 250
    val b: Array[Byte] = Array(67, 122, 0, 0)
    assertEquals(i.asBytes.toSeq, b.toSeq)
  }

  test("byte to float conversion") {
    assertEquals(0f, Array(0.toByte, 0.toByte, 0.toByte, 0.toByte).asFloat.get)
    assertEquals(1f, Array(0x3f.toByte, 0x80.toByte, 0x00.toByte, 0x00.toByte).asFloat.get)
    assertEquals(12.5f, Array(0x41.toByte, 0x48.toByte, 0x00.toByte, 0x00.toByte).asFloat.get)
    assertEquals(14.6f, Array(0x41.toByte, 0x69.toByte, 0x99.toByte, 0x9a.toByte).asFloat.get)
  }
}
