package forex.services.rates.interpreters

import java.util.concurrent.atomic.AtomicBoolean

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import forex.domain.{Rate, Timestamp}
import forex.services.rate.pairs.RatePairsAlgebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.Generators._
import forex.services.rates.interpreters.one.frame.OneFrameAlgebra
import forex.services.rates.interpreters.one.frame.Protocol.GetRatesSuccessfulResponse
import forex.services.time.TimeAlgebra
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.concurrent.ExecutionContext.global

class OneFrameCachedSpec extends AnyWordSpec with Matchers with OptionValues with ScalaCheckDrivenPropertyChecks {

  private implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(global)

  "OneFrameCached" should {

    "provide cached rate in case rate-ttl not yet elapsed" in {
      forAll(ratePairsAndGetRatesSuccessfulResponseGen, ratesCacheConfigGen) {
        case ((ratePairs, oneFrameLookupResponse), ratesCacheConfig) =>
          val oneFrameLookedUp = new AtomicBoolean(false)

          val oneFrameService =
            new OneFrameAlgebra[IO] {
              override def getRates(pairs: NonEmptyList[Rate.Pair]): IO[Error Either GetRatesSuccessfulResponse] =
                IO(
                  if (oneFrameLookedUp.compareAndSet(false, true)) Right(oneFrameLookupResponse)
                  else Left(Error.OneFrameLookupFailed("Quota reached"))
                )
            }

          val ratePairsService =
            RatePairsAlgebra.fromRatePairs[IO](ratePairs)

          val timeService =
            new TimeAlgebra[IO] {
              override def now: IO[Timestamp] =
                IO.pure(oneFrameLookupResponse.value.map(_.timeStamp).minBy(_.value))
            }

          val oneFrameCached = OneFrameCached[IO](
            oneFrameService,
            ratePairsService,
            timeService,
            ratesCacheConfig
          ).unsafeRunSync()

          forAll(ratePairGen(ratePairs)) {
            lookupPair =>
              val expectedRate =
                oneFrameLookupResponse.value.map(
                  oneFrameRate =>
                    Rate(
                      pair = Rate.Pair(oneFrameRate.from, oneFrameRate.to),
                      price = oneFrameRate.price,
                      timestamp = oneFrameRate.timeStamp
                    )
                ).find(_.pair == lookupPair).value

              oneFrameCached.get(lookupPair).unsafeRunSync() shouldBe Right(expectedRate)
          }
      }
    }

  }
}
