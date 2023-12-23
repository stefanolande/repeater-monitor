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
    val b: Array[Byte] = Array(67,122,0,0)
    assertEquals(i.asBytes.toSeq, b.toSeq)
  }
}
