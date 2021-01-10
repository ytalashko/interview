package forex.services.rates.interpreters

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.concurrent.{MVar, Semaphore}
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import forex.config.{OneFrameConfig, RatesCacheConfig}
import forex.domain.{Currency, Rate, Timestamp}
import forex.errors.InitializationError
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.one.frame.OneFrameClient
import forex.services.rates.interpreters.one.frame.cache.{OneFrameCacheAlgebra, RatesCache}
import org.http4s.client.Client

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

object OneFrameCached {

  def apply[F[_]: Concurrent](httpClient: Client[F],
                              oneFrameConfig: OneFrameConfig,
                              cacheConfig: RatesCacheConfig): F[Algebra[F]] =
    for {
      oneFrameClient   <- Concurrent[F].delay(OneFrameClient(httpClient, oneFrameConfig))
      allRatePairs     <- Concurrent[F].fromEither(possiblyAllRatePairs)
      cacheAlgebra      = OneFrameCacheAlgebra(oneFrameClient, allRatePairs)
      ratesMemo        <- MVar.of[F, Error Either RatesCache](Error.OneFrameCacheEmpty.asLeft)
      ratesUpdateMutex <- Semaphore[F](1)
      algebra           = new OneFrameCached(cacheAlgebra, ratesMemo, ratesUpdateMutex, cacheConfig)
    } yield algebra

  private def possiblyAllRatePairs: InitializationError Either NonEmptyList[Rate.Pair] = {
    val allPairs =
      for {
        from <- Currency.values
        to   <- Currency.values
        if from != to
        pair = Rate.Pair(from, to)
      } yield pair

    allPairs.toList.toNel
      .toRight(InitializationError("Too few supported currencies defined"))
  }

}
