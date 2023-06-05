import java.net.InetAddress
import pureconfig.*
import pureconfig.generic.derivation.default.*

case class ServiceConfiguration(
    arduinoPort: Int,
    arduinoIp: String,
    responseTimeout: Int
)

case class Configuration(service: ServiceConfiguration) derives ConfigReader
