package forex.services.rate.pairs

import cats.Applicative
import cats.data.{EitherT, NonEmptyList}
import cats.effect.Sync
import cats.syntax.list._
import forex.domain.{Currency, Rate}
import forex.services.rate.pairs.errors.Error

trait RatePairsAlgebra[F[_]] {
  def getSupportedRatePairs: F[NonEmptyList[Rate.Pair]]
}

object RatePairsAlgebra {

  def fromDefinedCurrencies[F[_]: Sync]: F[Error Either RatePairsAlgebra[F]] =
    EitherT(Sync[F].delay(possiblyAllRatePairs)).map(fromRatePairs[F]).value

  def fromRatePairs[F[_]: Applicative](pairs: NonEmptyList[Rate.Pair]): RatePairsAlgebra[F] =
    new RatePairsAlgebra[F] {
      override def getSupportedRatePairs: F[NonEmptyList[Rate.Pair]] =
        Applicative[F].pure(pairs)
    }

  private def possiblyAllRatePairs: Error Either NonEmptyList[Rate.Pair] = {
    val allPairs =
      for {
        from <- Currency.values
        to   <- Currency.values
        if from != to
        pair = Rate.Pair(from, to)
      } yield pair

    allPairs.toList.toNel
      .toRight(Error.TooFewCurrenciesDefined)
  }
}
