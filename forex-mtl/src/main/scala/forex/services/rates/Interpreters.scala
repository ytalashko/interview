package forex.services.rates

import cats.Applicative
import cats.effect.Concurrent
import forex.config.RatesCacheConfig
import forex.services.rates.interpreters._
import forex.services.rates.interpreters.one.frame.OneFrameAlgebra
import forex.services.{RatePairsService, TimeService}

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Concurrent](oneFrameService: OneFrameAlgebra[F],
                             ratePairsService: RatePairsService[F],
                             timeService: TimeService[F],
                             cacheConfig: RatesCacheConfig): F[Algebra[F]] =
    OneFrameCached(oneFrameService, ratePairsService, timeService, cacheConfig)

}
