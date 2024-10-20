//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-jsoniter-scala:1.11.7
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.31.0
//> using dep ch.qos.logback:logback-classic:1.5.11

import com.github.plokhotnyuk.jsoniter_scala.macros.* 

import sttp.tapir.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.shared.Identity

case class Result(v: Int) derives ConfiguredJsonValueCodec, Schema
case class Error(description: String) derives ConfiguredJsonValueCodec, Schema

@main def tapirErrors(): Unit = 
  val maybeErrorEndpoint = endpoint.get
    .in("test")
    .in(query[Int]("input"))
    .out(jsonBody[Result])
    .errorOut(jsonBody[Error])
    .handle { input =>
        if input % 3 == 0 then throw new RuntimeException("Multiplies of 3 are unacceptable!")
        if input % 2 == 0
        then Right(Result(input/2))
        else Left(Error("That's an odd number!"))
    }

  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[Identity](List(maybeErrorEndpoint), "My App", "1.0")

  NettySyncServer().port(8080)
    .addEndpoint(maybeErrorEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()