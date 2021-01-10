package forex

import scala.util.control.NoStackTrace

object errors {

  case class InitializationError(msg: String) extends Exception(msg) with NoStackTrace

}
