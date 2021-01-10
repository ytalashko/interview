package forex.services.rates.interpreters.one.frame

import java.time.OffsetDateTime

import enumeratum.Circe
import forex.domain.{Currency, Price, Timestamp}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

object Protocol {

  type GetRatesResponse = GetRatesErrorResponse Either GetRatesSuccessfulResponse

  final case class OneFrameRate(
      from: Currency,
      to: Currency,
      price: Price,
      timeStamp: Timestamp
  )

  final case class GetRatesSuccessfulResponse(value: List[OneFrameRate]) extends AnyVal

  final case class GetRatesErrorResponse(error: String) extends AnyVal

  implicit val decodingConfiguration: Configuration =
    Configuration.default.withSnakeCaseMemberNames

  implicit val currencyDecoder: Decoder[Currency] =
    Circe.decodeCaseInsensitive(Currency)

  implicit val priceDecoder: Decoder[Price] =
    Decoder[BigDecimal].map(Price.apply)

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder[OffsetDateTime].map(Timestamp.apply)

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] =
    deriveDecoder

  implicit val getRatesSuccessfulResponseDecoder: Decoder[GetRatesSuccessfulResponse] =
    deriveUnwrappedDecoder

  implicit val getRatesErrorResponseDecoder: Decoder[GetRatesErrorResponse] =
    deriveDecoder

  implicit val getRatesResponseDecoder: Decoder[GetRatesResponse] =
    getRatesErrorResponseDecoder.either(getRatesSuccessfulResponseDecoder)
}
