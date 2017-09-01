package models

import org.joda.time.DateTime
import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Json._
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }
import scala.concurrent.Await
import scala.concurrent.duration._
import reactivemongo.play.json._
import reactivemongo.bson.BSONObjectID

/*
* Stores the Auth Token information. Each token belongs to a Api Key and user
*/
case class ApiToken(
    token: String, // UUID 36 digits
    apiKey: String,
    expirationTime: DateTime,
    userId: BSONObjectID
) {
  def isExpired = expirationTime.isBeforeNow
}

object ApiToken {
  //import utils.DbDriver.database
  import utils.MONGODBDriver.db
  implicit val apiTokenFormat = Json.format[ApiToken]

  def collection: Future[JSONCollection] = db.map(_.collection[JSONCollection]("token_tbl"))

  def findByTokenAndApiKey(token: String, apiKey: String): Future[Option[ApiToken]] = {
    val newToken = collection.flatMap(_.find(obj("token" -> token, "apiKey" -> apiKey)).one[ApiToken])
    newToken.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully find token with result: $data")
    }
    newToken
  }

  def create(apiKey: String, userId: String): Future[String] = Future.successful {
    // Be sure the uuid is not already taken for another token
    def newUUID: String = {
      val uuid = UUID.randomUUID().toString
      val newToken = collection.flatMap(_.find(obj())
        .cursor[ApiToken](ReadPreference.primary)
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[ApiToken]]()))
      newToken.onComplete {
        case Failure(e) => e.printStackTrace()
        case Success(data) => println(s"Successfully find token with result: $data")
      }
      Await.result(newToken.map(tokens => if (!tokens.exists(_.token == uuid)) uuid else newUUID), 0.5.seconds)
    }
    val token = newUUID
    val writeRes = collection.flatMap(_.insert(ApiToken(token, apiKey, expirationTime = new DateTime() plusSeconds 3000, BSONObjectID.parse(userId).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully insert token with result: $writeResult")
    }
    token
  }

  def delete(token: String): Future[Unit] = Future.successful {
    val writeRes = collection.flatMap(_.remove(obj("token" -> token)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully delete token with result: $writeResult")
    }
  }
}