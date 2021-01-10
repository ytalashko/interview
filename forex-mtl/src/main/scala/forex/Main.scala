package forex

import cats.effect._
import cats.syntax.functor._
import forex.config._
import forex.services.RatePairsServices
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream.compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream: Stream[F, Unit] =
    for {
      config                   <- Config.stream("app")
      possiblyRatePairsService <- Stream.eval(RatePairsServices.fromDefinedCurrencies)
      ratePairsService         <- Stream.fromEither(possiblyRatePairsService)
      httpClient               <- Stream.resource(BlazeClientBuilder[F](global).resource)
      ratesService             <- Stream.eval(
        ModuleF.oneFrameCached(
          httpClient,
          ratePairsService,
          config.oneFrame,
          config.ratesCache
        )
      )
      module = new Module(ratesService, config)
      _ <- BlazeServerBuilder[F]
            .bindHttp(config.http.port.value, config.http.host.value)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
