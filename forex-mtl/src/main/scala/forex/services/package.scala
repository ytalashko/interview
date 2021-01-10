package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type RatePairsService[F[_]] = rate.pairs.RatePairsAlgebra[F]
  final val RatePairsServices = rate.pairs.Interpreters

  type TimeService[F[_]] = time.TimeAlgebra[F]
  final val TimeServices = time.Interpreters
}
