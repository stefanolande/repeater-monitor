package model

import cats.data.NonEmptyList

import scala.util.Try

case class APRSTelemetry(path: String, sequence: Int, values: NonEmptyList[Double], bits: String)

object APRSTelemetry {
  def parse(string: String): Option[APRSTelemetry] =
    string.split(':') match {
      case Array(path, message) if message.startsWith("T#") =>
        val values = message.split(',')
        if (values.length == 7) {
          Try {
            val sequence = values.head.split('#')(1).toInt
            val numbers  = values.drop(1).dropRight(1).toList.map(_.toDouble)
            APRSTelemetry(path, sequence, NonEmptyList.fromListUnsafe(numbers), values.last.filter(_ >= ' '))
          }.toOption
        } else None
      case _ => None
    }
}
