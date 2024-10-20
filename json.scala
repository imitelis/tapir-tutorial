//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.7
//> using dep com.softwaremill.sttp.tapir::tapir-jsoniter-scala:1.11.7
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.31.0

import com.github.plokhotnyuk.jsoniter_scala.macros.* // needed for ... derives

import sttp.tapir.*
import sttp.tapir.json.jsoniter.* // needed for jsonBody[T]
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.shared.Identity

import scala.util.Random

case class Meal(name: String, servings: Int, ingredients: List[String])
  derives ConfiguredJsonValueCodec, Schema
case class Nutrition(name: String, healthy: Boolean, calories: Int)
  derives ConfiguredJsonValueCodec, Schema

@main def tapirJson(): Unit = 
  val random = new Random
  
  val mealEndpoint = endpoint.post
    .in(jsonBody[Meal])
    .out(jsonBody[Nutrition])
    // plugging in AI is left as an exercise to the reader
    .handleSuccess { meal => 
      Nutrition(meal.name, random.nextBoolean(), random.nextInt(1000)) 
    }

  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[Identity](List(mealEndpoint), "My App", "1.0")
 
  NettySyncServer().port(8080)
    .addEndpoint(mealEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()
