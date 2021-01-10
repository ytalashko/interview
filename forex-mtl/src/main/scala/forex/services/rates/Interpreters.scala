package forex.services.rates

import cats.Applicative
import cats.effect.Concurrent
import forex.config.{OneFrameConfig, RatesCacheConfig}
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Concurrent](httpClient: Client[F],
                             oneFrameConfig: OneFrameConfig,
                             cacheConfig: RatesCacheConfig): F[Algebra[F]] =
    OneFrameCached(httpClient, oneFrameConfig, cacheConfig)
}
