package forex.domain

import cats.Show
import enumeratum.EnumEntry.Uppercase
import enumeratum.{Cats, Enum, EnumEntry}

import scala.collection.immutable

sealed trait Currency extends EnumEntry with Uppercase

object Currency extends Enum[Currency] {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  implicit val show: Show[Currency] =
    Cats.showForEnum

  // Q: Do we need `maybeFromString` wrapper function at all?
  //    For me, it provides a nicer API/usage view
  @inline def maybeFromString(s: String): Option[Currency] =
    withNameInsensitiveOption(s)

  override val values: immutable.IndexedSeq[Currency] =
    findValues

}
