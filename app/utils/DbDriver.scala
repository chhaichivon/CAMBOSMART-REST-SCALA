package utils

import com.google.inject.Singleton
import reactivemongo.api.{ MongoConnection, MongoDriver }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
object DbDriver {
  val mongoUri = "mongodb://localhost:27017/cambosmart"
  val driver = new MongoDriver
  val database = for {
    uri <- Future.fromTry(MongoConnection.parseURI(mongoUri))
    con = driver.connection(uri)
    dn <- Future(uri.db.get)
    db <- con.database(dn)
  } yield db
}
