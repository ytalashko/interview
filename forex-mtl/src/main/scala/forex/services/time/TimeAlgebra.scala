package forex.services.time

import cats.effect.Sync
import forex.domain.Timestamp

trait TimeAlgebra[F[_]] {
  def now: F[Timestamp]
}

object TimeAlgebra {

  def live[F[_]: Sync]: TimeAlgebra[F] =
    new TimeAlgebra[F] {
      override def now: F[Timestamp] =
        Sync[F].delay(Timestamp.now)
    }

}
