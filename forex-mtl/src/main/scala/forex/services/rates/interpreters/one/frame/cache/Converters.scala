package forex.services.rates.interpreters.one.frame.cache

import forex.domain.Rate
import forex.services.rates.interpreters.one.frame.Protocol

object Converters {
  import Protocol._

  private[interpreters] implicit class RatesCacheOps(val response: GetRatesSuccessfulResponse) {
    def toRatesCache: RatesCache =
      RatesCache(
        response.value.map {
          oneFrameRate =>
            val pair = Rate.Pair(oneFrameRate.from, oneFrameRate.to)
            val rate = Rate(pair, oneFrameRate.price, oneFrameRate.timeStamp)

            pair -> rate
        }.toMap
      )
  }

}

