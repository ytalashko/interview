package forex.services.rates.interpreters

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.{MVar, Semaphore}
import cats.implicits._
import forex.config.RatesCacheConfig
import forex.domain.{Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.one.frame.cache.{OneFrameCacheAlgebra, RatesCache}

class OneFrameCached[F[_]: Sync](oneFrameCache: OneFrameCacheAlgebra[F],
                                 ratesMemo: MVar[F, Error Either RatesCache],
                                 ratesUpdateMutex: Semaphore[F],
                                 cacheConfig: RatesCacheConfig) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    getValidCachedRateOr(
      pair,
      Sync[F].bracket(
        ratesUpdateMutex.acquire
      )(
        _ => getValidCachedRateOr(pair, getUpdatedCachedRate(pair))
      )(
        _ => ratesUpdateMutex.release
      )
    )

  private def getValidCachedRateOr(pair: Rate.Pair,
                                   updateAndGetCachedRate: F[Error Either Rate]): F[Error Either Rate] =
    for {
      cachedRate        <- getCachedRate(pair)
      updatedCachedRate <-
        if (containsValidRate(cachedRate)) Applicative[F].pure(cachedRate)
        else updateAndGetCachedRate
    } yield updatedCachedRate

  private def getUpdatedCachedRate(pair: Rate.Pair): F[Error Either Rate] =
    for {
      _           <- updateCache()
      updatedRate <- getCachedRate(pair)
    } yield updatedRate

  private def updateCache(): F[Unit] =
    for {
      cachedRates <- oneFrameCache.createRatesCache()
      _           <- ratesMemo.take
      _           <- ratesMemo.put(cachedRates)
    } yield ()

  private def getCachedRate(pair: Rate.Pair): F[Error Either Rate] =
    for {
      cachedRates <- ratesMemo.read
      cachedRate   = cachedRates.flatMap(
        _.value
          .get(pair)
          .toRight(Error.OneFrameCacheMissingPair(pair))
      )
    } yield cachedRate

  private def containsValidRate(possiblyCachedRate: Error Either Rate): Boolean =
    possiblyCachedRate.exists(
      _.timestamp.value.plusNanos(cacheConfig.rateTtl.value.toNanos) isAfter Timestamp.now.value
    )

}
