package forex.config

import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    ratesCache: RatesCacheConfig
)

case class ApplicationHost(value: String) extends AnyVal
case class ApplicationPort(value: Int) extends AnyVal
case class HttpRequestTimeout(value: FiniteDuration) extends AnyVal
case class HttpConfig(
    host: ApplicationHost,
    port: ApplicationPort,
    timeout: HttpRequestTimeout
)

// Q: I don't want to couple config with external lib specific ADT (e.g. http4s.Uri),
//     but even more, want to provide evidence of a valid value
case class OneFrameBaseUrl(value: Uri) extends AnyVal
case class OneFrameAuthenticationToken(value: String) extends AnyVal
case class OneFrameConfig(
    baseUrl: OneFrameBaseUrl,
    authenticationToken: OneFrameAuthenticationToken
)

case class RateTTL(value: FiniteDuration) extends AnyVal
case class RatesCacheConfig(rateTtl: RateTTL)
