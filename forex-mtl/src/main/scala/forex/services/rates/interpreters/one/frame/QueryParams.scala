package forex.services.rates.interpreters.one.frame

import cats.syntax.show._
import forex.domain.Rate
import org.http4s.{QueryParam, QueryParamEncoder}

object QueryParams {

  private[frame] implicit val ratePairQueryParam: QueryParam[Rate.Pair] =
    QueryParam.fromKey("pair")

  // Don't want to define specific `Show[Rate.Pair]`, because the definition will not exactly show
  //  how pair should be shown in general,
  //  but just how we should pass it value to the one-frame API
  private[frame] implicit val ratePairQueryParamEncoder: QueryParamEncoder[Rate.Pair] =
    QueryParamEncoder[String].contramap(
      pair =>
        show"${pair.from}${pair.to}"
    )

}
