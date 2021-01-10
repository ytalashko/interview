package forex.services.rate.pairs

import cats.syntax.show._
import forex.domain.Currency

import scala.util.control.NoStackTrace

object errors {

  sealed trait Error extends Exception with NoStackTrace
  object Error {
    final case object TooFewCurrenciesDefined extends Error {
      override def getMessage: String =
        s"Too few currencies defined, expected at least 2 currencies," +
          s" but defined ${Currency.values.length}: ${Currency.values.map(_.show).mkString("[", ", ", "]")}"
    }
  }

}
