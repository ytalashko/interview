package forex.http.rates

import forex.domain.Currency
import forex.programs.rates.errors.{ Error => RatesProgramError }

import scala.util.control.NoStackTrace

object errors {

  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class RatesEndpointInvalidQueryParam(name: QueryParamName, details: String) extends Error
    final case class RatesEndpointMissingQueryParam(name: QueryParamName) extends Error

    final case class RatesEndpointFromEqualTo(value: Currency) extends Error

    final case class RatesProviderCommunicationFailed(msg: String) extends Error
    final case class RatesEndpointProgramFailed(msg: String) extends Error
  }

  def toHttpError(error: RatesProgramError): Error = error match {
    case RatesProgramError.RatesProviderLookupFailed(msg)          => Error.RatesProviderCommunicationFailed(msg)
    case RatesProgramError.RatesCacheLookupFailed(msg)             => Error.RatesEndpointProgramFailed(msg)
    case RatesProgramError.RatesProviderResponseParsingFailed(msg) => Error.RatesEndpointProgramFailed(msg)
  }

}
