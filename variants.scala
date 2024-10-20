//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.7

import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.shared.Identity

enum AvatarError:
  case Unauthorized
  case NotFound
  case Other(msg: String)

enum AvatarSuccess:
  case Found(bytes: Array[Byte])
  case Redirect(location: String)

val o1: EndpointOutput[Unit] = statusCode(StatusCode.TemporaryRedirect)
val o2: EndpointOutput[String] = header[String](HeaderNames.Location)

val successOutput: EndpointOutput[AvatarSuccess] = oneOf(
  oneOfVariant(o1.and(o2).mapTo[AvatarSuccess.Redirect]),
  oneOfVariant(byteArrayBody.mapTo[AvatarSuccess.Found])
)

val errorOutput: EndpointOutput[AvatarError] = oneOf(
  oneOfVariantSingletonMatcher(statusCode(StatusCode.Unauthorized))(AvatarError.Unauthorized),
  oneOfVariantSingletonMatcher(statusCode(StatusCode.NotFound))(AvatarError.NotFound),
  oneOfVariant(stringBody.mapTo[AvatarError.Other])
)

@main def tapirErrorVariants(): Unit =
  val avatarEndpoint = endpoint.get
    .in("user" / "avatar")
    .in(query[Int]("id"))
    .out(successOutput)
    .errorOut(errorOutput)
    // Int => Either[AvatarError, AvatarSuccess]
    .handle {
      case 1 => Right(AvatarSuccess.Found(":-)".getBytes))
      case 2 => Right(AvatarSuccess.Redirect("https://example.org/me.jpg"))
      case 3 => Left(AvatarError.Unauthorized)
      case 4 => Left(AvatarError.Other("We don't like this user."))
      case _ => Left(AvatarError.NotFound)
    }

  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[Identity](
    List(avatarEndpoint), "My App", "1.0")

  NettySyncServer()
    .port(8080)
    .addEndpoint(avatarEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()