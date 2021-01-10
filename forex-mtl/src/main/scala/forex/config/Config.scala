package forex.config

import cats.effect.Sync
import cats.syntax.either._
import fs2.Stream
import org.http4s.Uri
import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._

object Config {

  // Q: `ConfigReader.fromNonEmptyString` can be used instead to separately treat empty uri case as `EmptyStringFound`,
  //      but empty string is the same invalid value as any other
  private[config] implicit val uriReader: ConfigReader[Uri] =
    ConfigReader[String].emap(
      configValue =>
        Uri
          .fromString(configValue)
          .leftMap(error => CannotConvert(configValue, classOf[Uri].getName, error.message))
    )

  /**
   * @param path the property path inside the default configuration
   */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] = {
    Stream.eval(Sync[F].delay(
      ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))
  }

}
