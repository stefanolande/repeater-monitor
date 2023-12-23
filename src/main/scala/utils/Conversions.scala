package utils

import cats.implicits.*

import java.nio.{ByteBuffer, ByteOrder}

object Conversions {
  extension (value: Int) {
    def asBytes: Array[Byte] = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array

  }

  extension (value: Float) {
    def asBytes: Array[Byte] = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(value).array
  }

  extension (value: Array[Byte]) {
    def asInt: Option[Int] =
      if (value.length != 4)
        None
      else
        ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getInt.some

    def asFloat: Option[Float] =
      if (value.length != 4)
        None
      else
        ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getFloat.some
  }
}
