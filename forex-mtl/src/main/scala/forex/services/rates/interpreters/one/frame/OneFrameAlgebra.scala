package forex.services.rates.interpreters.one.frame

import cats.data.NonEmptyList
import forex.domain.Rate
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.one.frame.Protocol.GetRatesSuccessfulResponse

trait OneFrameAlgebra[F[_]] {
  def getRates(pairs: NonEmptyList[Rate.Pair]): F[Error Either GetRatesSuccessfulResponse]
}
