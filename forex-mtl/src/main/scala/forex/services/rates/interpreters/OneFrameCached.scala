package forex.services.rates.interpreters

import cats.Applicative
import cats.data.EitherT
import cats.effect.concurrent.{MVar, Semaphore}
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import forex.config.RatesCacheConfig
import forex.domain.{Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.one.frame.OneFrameAlgebra
import forex.services.rates.interpreters.one.frame.cache.RatesCache
import forex.services.{RatePairsService, TimeService}

class OneFrameCached[F[_]: Sync](oneFrameService: OneFrameAlgebra[F],
                                 ratePairsService: RatePairsService[F],
                                 timeService: TimeService[F],
                                 ratesMemo: MVar[F, Error Either RatesCache],
                                 ratesUpdateMutex: Semaphore[F],
                                 cacheConfig: RatesCacheConfig) extends Algebra[F] {

  import one.frame.cache.Converters._

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
      now               <- timeService.now
      updatedCachedRate <-
        if (containsValidRate(cachedRate, now)) Applicative[F].pure(cachedRate)
        else updateAndGetCachedRate
    } yield updatedCachedRate

  private def getUpdatedCachedRate(pair: Rate.Pair): F[Error Either Rate] =
    for {
      _           <- updateCache()
      updatedRate <- getCachedRate(pair)
    } yield updatedRate

  private def updateCache(): F[Unit] =
    for {
      supportedRatePairs <- ratePairsService.getSupportedRatePairs
      cachedRates        <- EitherT(oneFrameService.getRates(supportedRatePairs)).map(_.toRatesCache).value
      _                  <- ratesMemo.take
      _                  <- ratesMemo.put(cachedRates)
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

  private def containsValidRate(possiblyCachedRate: Error Either Rate, now: Timestamp): Boolean =
    possiblyCachedRate.exists(
      _.timestamp.value.plusNanos(cacheConfig.rateTtl.value.toNanos) isAfter now.value
    )

}

object OneFrameCached {

  def apply[F[_]: Concurrent](oneFrameService: OneFrameAlgebra[F],
                              ratePairsService: RatePairsService[F],
                              timeService: TimeService[F],
                              cacheConfig: RatesCacheConfig): F[Algebra[F]] =
    for {
      ratesMemo        <- MVar.of[F, Error Either RatesCache](Error.OneFrameCacheEmpty.asLeft)
      ratesUpdateMutex <- Semaphore[F](1)
      algebra           = new OneFrameCached(
        oneFrameService,
        ratePairsService,
        timeService,
        ratesMemo,
        ratesUpdateMutex,
        cacheConfig
      )
    } yield algebra

}
