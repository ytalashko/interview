package forex.services.rates

import forex.domain.Rate

import scala.util.control.NoStackTrace

object errors {

  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case class OneFrameResponseDecodingFailed(msg: String) extends Error
    final case class OneFrameLookupFailed(msg: String) extends Error

    final case class OneFrameCacheMissingPair(pair: Rate.Pair) extends Error
    final case object OneFrameCacheEmpty extends Error
  }

}
