package forex.programs.rates

import cats.syntax.show._
import forex.services.rates.errors.{ Error => RatesServiceError }

import scala.util.control.NoStackTrace

object errors {

  // Q: Why not `with NoStackTrace` ?
  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class RatesCacheLookupFailed(msg: String) extends Error
    final case class RatesProviderLookupFailed(msg: String) extends Error
    final case class RatesProviderResponseParsingFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameResponseDecodingFailed(msg) => Error.RatesProviderResponseParsingFailed(s"Cannot decode one frame service response: $msg")
    case RatesServiceError.OneFrameLookupFailed(msg)           => Error.RatesProviderLookupFailed(s"Got failure communicating to one frame service: $msg")
    case RatesServiceError.OneFrameCacheMissingPair(pair)      => Error.RatesCacheLookupFailed(show"No cached rate found between currencies $pair")
    case RatesServiceError.OneFrameCacheEmpty                  => Error.RatesCacheLookupFailed("Cannot initialize rates cache")
  }
}
