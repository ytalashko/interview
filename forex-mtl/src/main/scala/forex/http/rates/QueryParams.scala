package forex.http.rates

import cats.data.ValidatedNel
import cats.syntax.either._
import cats.syntax.show._
import forex.domain.Currency
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}

object QueryParams {

  //TODO: `use QueryParamDecoder[String].emap(...)` after http4s upgrade to 0.21.0 or higher
  private[http] implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    new QueryParamDecoder[Currency] {
      def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, Currency] =
        Currency
          .maybeFromString(value.value)
          //Q: also, may be just `not supported`
          .toRight(ParseFailure("Invalid currency", s"${value.value} is not a valid currency"))
          .toValidatedNel
    }

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency](QueryParamName.From.show)
  object ToQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency](QueryParamName.To.show)

}
