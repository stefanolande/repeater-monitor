package services

object Conversions {
  extension (l: Int) {
    def asBytes: Array[Byte] = Array(l & 0xff, (l >> 8) & 0xff, (l >> 16) & 0xff, (l >> 24) & 0xff).map(_.toByte)
  }

  extension (s: Short) {
    def asBytes: Array[Byte] = Array(s & 0xff, (s >> 8) & 0xff).map(_.toByte)
  }
}
