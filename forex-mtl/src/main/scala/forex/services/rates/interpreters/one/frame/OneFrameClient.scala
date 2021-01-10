package forex.services.rates.interpreters.one.frame

import cats.Applicative
import cats.data.{EitherT, NonEmptyList}
import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.either._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.rates.errors.Error
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Header, Request, Response}

import scala.util.control.NonFatal

class OneFrameClient[F[_]: Sync](httpClient: Client[F],
                                 config: OneFrameConfig) extends OneFrameAlgebra[F] with Http4sClientDsl[F] {

  import QueryParams._, Protocol._
  import org.http4s.circe.CirceEntityDecoder._

  //TODO: logging around third-party service interactions
  override def getRates(pairs: NonEmptyList[Rate.Pair]): F[Error Either GetRatesSuccessfulResponse] =
    EitherT(httpClient.expectOr[GetRatesResponse](createRatesRequest(pairs))(oneFrameLookupFailed)).leftMap(toError).value.recover {
      case NonFatal(error @ Error.OneFrameLookupFailed(_)) => error.asLeft
      case NonFatal(error)                                 => Error.OneFrameResponseDecodingFailed(error.getMessage).asLeft
    }

  private def createRatesRequest(pairs: NonEmptyList[Rate.Pair]): F[Request[F]] =
    GET(
      config.baseUrl.value / "rates" +*? pairs.toList,
      // Q: Should we move headers related operations into `Headers` object, similarly to `QueryParams`?
      Header("token", config.authenticationToken.value)
    )

  private def toError(error: GetRatesErrorResponse): Error =
    Error.OneFrameLookupFailed(error.error)

  private def oneFrameLookupFailed(from: Response[F]): F[Throwable] =
    Applicative[F].pure(Error.OneFrameLookupFailed(s"Got unexpected HTTP response with status ${from.status}"))

}

object OneFrameClient {

  def apply[F[_]: Sync](httpClient: Client[F],
                        config: OneFrameConfig): OneFrameClient[F] =
    new OneFrameClient(httpClient, config)

}
