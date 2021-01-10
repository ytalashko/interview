package forex.services.rates.interpreters.one.frame.cache

import cats.Functor
import cats.data.{EitherT, NonEmptyList}
import forex.domain.Rate
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.one.frame.OneFrameClient

trait OneFrameCacheAlgebra[F[_]] {

  def createRatesCache(): F[Error Either RatesCache]
}

object OneFrameCacheAlgebra {
  import Converters._

  def apply[F[_]: Functor](oneFrameClient: OneFrameClient[F],
                           supportedRatePairs: NonEmptyList[Rate.Pair]): OneFrameCacheAlgebra[F] =
    new OneFrameCacheAlgebra[F] {
      override def createRatesCache(): F[Error Either RatesCache] =
        EitherT(oneFrameClient.getRates(supportedRatePairs)).map(_.toRatesCache).value
    }

}
