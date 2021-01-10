package forex

import cats.effect.Concurrent
import cats.implicits._
import forex.config.{OneFrameConfig, RatesCacheConfig}
import forex.services.rates.interpreters.one.frame.OneFrameClient
import forex.services.{RatePairsService, RatesService, RatesServices, TimeServices}
import org.http4s.client.Client

object ModuleF {

  def oneFrameCached[F[_]: Concurrent](httpClient: Client[F],
                                       ratePairsService: RatePairsService[F],
                                       oneFrameConfig: OneFrameConfig,
                                       cacheConfig: RatesCacheConfig): F[RatesService[F]] =
    for {
      oneFrameService <- Concurrent[F].delay(OneFrameClient(httpClient, oneFrameConfig))
      timeService     <- Concurrent[F].delay(TimeServices.live)
      oneFrameService <- RatesServices.live(
        oneFrameService,
        ratePairsService,
        timeService,
        cacheConfig
      )
    } yield oneFrameService

}
