package utils

import com.google.inject.Singleton

import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

@Singleton()
object MONGODBDriver {
  //val mongoUri = "mongodb://localhost:27017/mydb?authMode=scram-sha1"
  val mongoUri = "mongodb://localhost:27017"
  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db: Future[DefaultDB] = futureConnection.flatMap(_.database("cambosmart"))
}

