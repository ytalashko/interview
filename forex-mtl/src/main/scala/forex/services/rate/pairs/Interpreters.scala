package forex.services.rate.pairs

import cats.effect.Sync
import forex.services.rate.pairs.errors.Error

object Interpreters {
  def fromDefinedCurrencies[F[_]: Sync]: F[Error Either RatePairsAlgebra[F]] =
    RatePairsAlgebra.fromDefinedCurrencies
}
