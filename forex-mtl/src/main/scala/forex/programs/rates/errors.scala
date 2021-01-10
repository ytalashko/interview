package forex.programs.rates

import cats.syntax.show._
import forex.services.rates.errors.{ Error => RatesServiceError }

import scala.util.control.NoStackTrace

object errors {

  // Q: Why not `with NoStackTrace` ?
  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RatesCacheLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameCacheMissingPair(pair)      => Error.RatesCacheLookupFailed(show"No cached rate found between currencies $pair")
    case RatesServiceError.OneFrameResponseDecodingFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.OneFrameLookupFailed(msg)           => Error.RateLookupFailed(msg)
  }
}
