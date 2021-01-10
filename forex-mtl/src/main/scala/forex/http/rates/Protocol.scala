package forex.http
package rates

import enumeratum.Circe
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.semiauto._

object Protocol {

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Circe.encoder(Currency)

  implicit val pairEncoder: Encoder[Pair] =
    deriveEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveEncoder[GetApiResponse]

}
