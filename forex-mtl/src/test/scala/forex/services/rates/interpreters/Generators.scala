package forex.services.rates.interpreters

import java.time.{Instant, ZoneOffset}

import cats.data.NonEmptyList
import forex.config.{RateTTL, RatesCacheConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.interpreters.one.frame.Protocol._
import org.scalacheck.Gen
import org.scalacheck.Gen.chooseNum

import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration}

object Generators {

  private val allRatePairs: NonEmptyList[Rate.Pair] =
    NonEmptyList.fromListUnsafe(
      for {
        from <- Currency.values.toList
        to <- Currency.values
        if from != to
        pair = Rate.Pair(from, to)
      } yield pair
    )

  val allRatePairsGen: Gen[NonEmptyList[Rate.Pair]] =
    Gen.const(allRatePairs)

  val ratePairsGen: Gen[NonEmptyList[Rate.Pair]] =
    for {
      pairs <- Gen.atLeastOne(allRatePairs.toList)
      pairsNel = NonEmptyList.fromListUnsafe(pairs.toList)
    } yield pairsNel

  def ratePairGen(from: NonEmptyList[Rate.Pair]): Gen[Rate.Pair] =
    Gen.oneOf(from.toList)

  val priceGen: Gen[Price] =
    for {
      priceValue <- Gen.posNum[Double]
      price = Price(BigDecimal(priceValue))
    } yield price

  def timestampWithinTtlGen(startingFrom: Timestamp, ttl: FiniteDuration): Gen[Timestamp] =
    for {
      plusNanos <- Gen.chooseNum(0L, ttl.toNanos - 1L)
      resultTimestamp = Timestamp(startingFrom.value.plusNanos(plusNanos))
    } yield resultTimestamp

  //TODO: generate not only UTC timezone
  val timestampGen: Gen[Timestamp] =
    for {
      timestampMillis <- Gen.chooseNum(0L, System.currentTimeMillis())
      timestamp = Timestamp(Instant.ofEpochMilli(timestampMillis).atZone(ZoneOffset.UTC).toOffsetDateTime)
    } yield timestamp

  def oneFrameRateGen(pair: Rate.Pair): Gen[OneFrameRate] =
    for {
      price <- priceGen
      timestamp <- timestampGen
      oneFrameRate = OneFrameRate(pair.from, pair.to, price, timestamp)
    } yield oneFrameRate

  val ratePairsAndGetRatesSuccessfulResponseGen: Gen[(NonEmptyList[Rate.Pair], GetRatesSuccessfulResponse)] =
    for {
      ratePairs <- ratePairsGen
      oneFrameRates <- Gen.sequence[List[OneFrameRate], OneFrameRate](ratePairs.toList.map(oneFrameRateGen))
      getRatesResponse = GetRatesSuccessfulResponse(oneFrameRates)
    } yield ratePairs -> getRatesResponse

  val ratesCacheConfigGen: Gen[RatesCacheConfig] =
    for {
      rateTtlValue <- chooseNum(30.seconds.toNanos, 5.minutes.toNanos).map(Duration.fromNanos)
      config        = RatesCacheConfig(RateTTL(rateTtlValue))
    } yield config
}
