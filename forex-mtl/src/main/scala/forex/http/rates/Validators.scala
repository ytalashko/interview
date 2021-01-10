package forex.http.rates

import cats.data.Validated
import cats.syntax.either._
import forex.http.rates.errors.Error

object Validators {
  import Protocol._

  private[rates] implicit class GetApiRequestValidationOps(val request: GetApiRequest) extends AnyVal {
    def validated: Validated[Error, GetApiRequest] =
      Either.cond(
        request.from != request.to,
        request,
        Error.RatesEndpointFromEqualTo(request.from)
      ).toValidated
  }

}
