package forex.http.rates

import cats.Show
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Cats, Enum, EnumEntry}

import scala.collection.immutable

sealed trait QueryParamName extends EnumEntry with Lowercase

object QueryParamName extends Enum[QueryParamName] {
  case object From extends QueryParamName
  case object To extends QueryParamName

  implicit val baseShow: Show[QueryParamName] =
    Cats.showForEnum

  implicit def show[E <: QueryParamName]: Show[E] =
    Show.show(baseShow.show)

  override val values: immutable.IndexedSeq[QueryParamName] =
    findValues

}
