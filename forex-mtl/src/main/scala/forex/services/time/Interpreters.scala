package forex.services.time

import cats.effect.Sync

object Interpreters {
  def live[F[_]: Sync]: TimeAlgebra[F] =
    TimeAlgebra.live
}
