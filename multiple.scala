//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.11.7

import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.shared.Identity

@main def tapirMultiple(): Unit =
  case class Input(opName: String, value1: Int, value2: Int)
  case class Output(result: String, hash: String)

  def hash(result: Int): Output =
    Output(result.toString, 
      scala.util.hashing.MurmurHash3.stringHash(result.toString).toString)

  val opEndpoint = endpoint.get
    .in("operation" / path[String]("opName"))
    .in(query[Int]("value1"))
    .in(query[Int]("value2"))
    .mapInTo[Input]
    .out(stringBody)
    .out(header[String]("X-Result-Hash"))
    .mapOutTo[Output]
    .errorOut(stringBody)
    // Input => Either[String, Output]
    .handle { input =>
      input.opName match
        case "add" => Right(hash(input.value1 + input.value2))
        case "sub" => Right(hash(input.value1 - input.value2))
        case _     => Left("Unknown operation. Available operations: add, sub")
    }

  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[Identity](List(opEndpoint), "My App", "1.0")

  NettySyncServer()
    .port(8080)
    .addEndpoint(opEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()