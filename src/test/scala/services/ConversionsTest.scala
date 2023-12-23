package services

import munit.FunSuite
import utils.Conversions._

class ConversionsTest extends FunSuite {

  test("Int to byte conversion") {
    val i              = 13800
    val b: Array[Byte] = Array(-24, 53, 0, 0)
    assertEquals(i.asBytes.toSeq, b.toSeq)
  }

  test("Short to byte conversion") {
    val i: Short       = 250
    val b: Array[Byte] = Array(-6, 0)
    assertEquals(i.asBytes.toSeq, b.toSeq)
  }
}
