package forex.services.rates.interpreters.one.frame.cache

import forex.domain.Rate

case class RatesCache(value: Map[Rate.Pair, Rate]) extends AnyVal
