package forex.http
package rates

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import errors._
import forex.programs.RatesProgram
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, Validators._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private type RoutesPF = PartialFunction[Request[F], F[Response[F]]]

  private def errorHandler(error: Throwable): F[Response[F]] = error match {
    case Error.RatesEndpointInvalidQueryParam(name, details) =>
      BadRequest(show"Got invalid query parameter '$name' value: $details")
    case Error.RatesEndpointMissingQueryParam(name)          =>
      BadRequest(show"Request missing query parameter $name")
    case Error.RatesEndpointFromEqualTo(currency)            =>
      BadRequest(show"Query parameters '${QueryParamName.From}' and '${QueryParamName.To}' must specify different currencies, but got '$currency' for both")
    case Error.RatesProviderCommunicationFailed(msg)               =>
      BadGateway(s"Currently cannot process rates request: $msg")
    case Error.RatesEndpointProgramFailed(msg)               =>
      InternalServerError(s"Cannot process rates request: $msg")
    case error                                               =>
      InternalServerError(s"Unexpected error occurred: ${error.getMessage}")
  }

  private def handleErrors(pf: RoutesPF): RoutesPF =
    pf.andThen(_.handleErrorWith(errorHandler))

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    handleErrors {
      case GET -> Root :? FromQueryParam(maybeFrom) +& ToQueryParam(maybeTo) =>
        for {
          validFrom    <- Sync[F].fromOptionalValidated(maybeFrom, QueryParamName.From)
          validTo      <- Sync[F].fromOptionalValidated(maybeTo, QueryParamName.To)
          validRequest <- Sync[F].fromValidated(GetApiRequest(validFrom, validTo).validated)
          possiblyRate <- EitherT(rates.get(validRequest.asGetRatesRequest)).leftMap(toHttpError).value
          rate         <- Sync[F].fromEither(possiblyRate)
          response     <- Ok(rate.asGetApiResponse)
        } yield response
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
