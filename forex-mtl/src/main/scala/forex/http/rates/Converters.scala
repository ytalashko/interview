package forex.http.rates

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import forex.domain._
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import forex.http.rates.errors.Error
import org.http4s.ParseFailure

object Converters {
  import Protocol._

  private[rates] implicit class GetRatesRequestOps(val apiRequest: GetApiRequest) extends AnyVal {
    def asGetRatesRequest: RatesProgramProtocol.GetRatesRequest =
      RatesProgramProtocol.GetRatesRequest(
        from = apiRequest.from,
        to = apiRequest.to
      )
  }

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }

  private[rates] implicit class QueryParamValidationOps[F[_]](val syncF: Sync[F]) extends AnyVal {
    @inline private implicit def implicitSyncF: Sync[F] = syncF

    def fromOptionalValidated[V](maybeValue: Option[ValidatedNel[ParseFailure, V]],
                                 queryParamName: QueryParamName): F[V] =
      for {
        value         <- syncF.fromOption(maybeValue, Error.RatesEndpointMissingQueryParam(queryParamName))
        validatedValue = value.leftMap(errors => Error.RatesEndpointInvalidQueryParam(queryParamName, errors.head.sanitized))
        validValue    <- syncF.fromValidated(validatedValue)
      } yield validValue
  }
}
