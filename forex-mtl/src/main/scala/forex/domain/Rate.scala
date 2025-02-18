package forex.domain

import cats.Show
import cats.syntax.show._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {

    implicit val show: Show[Pair] =
      Show.show(
        pair =>
          show"{from: ${pair.from}, to: ${pair.to}"
      )

  }
}
