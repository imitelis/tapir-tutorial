//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.11.7

import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer

@main def helloWorldTapir(): Unit =
  val helloWorldEndpoint = endpoint
    .get
    .in("hello" / "world")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess(name => s"Hello, $name!")

  NettySyncServer()
    .port(8080)
    .addEndpoint(helloWorldEndpoint)
    .startAndWait()
