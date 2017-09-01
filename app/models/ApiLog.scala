package models

import api.ApiRequestHeader
import play.api.mvc.RequestHeader
import play.api.libs.json._
import play.api.libs.json.Json
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }
import reactivemongo.play.json._

/*
* Stores all the information of a request. Specially used for store the errors in the DB.
*/
case class ApiLog(
    _id: Option[BSONObjectID],
    date: DateTime,
    ip: String,
    apiKey: Option[String],
    token: Option[String],
    method: String,
    uri: String,
    requestBody: Option[String],
    responseStatus: Int,
    responseBody: Option[String]
) {
  def dateStr: String = ApiLog.dtf.print(date)
}
object ApiLog {
  //import utils.DbDriver.database
  import utils.MONGODBDriver.db
  implicit val apiLogFormat = Json.format[ApiLog]

  private val dtf = DateTimeFormat.forPattern("MM/dd/yyyy HH:ss:mm")

  def collection: Future[JSONCollection] = db.map(_.collection[JSONCollection]("apiLog_tbl"))
  def findById(id: String): Future[Option[ApiLog]] = {
    val apiLog = collection.flatMap(_.find(obj("_id" -> BSONObjectID.parse(id).get)).one[ApiLog])
    apiLog.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully find token with result: $data")
    }
    apiLog
  }

  def insert[R <: RequestHeader](request: ApiRequestHeader[R], status: Int, json: JsValue): Future[ApiLog] = {
    val log = ApiLog(
      Some(BSONObjectID.generate()),
      date = new DateTime(),
      ip = request.remoteAddress,
      apiKey = request.apiKeyOpt,
      token = request.tokenOpt,
      method = request.method,
      uri = request.uri,
      requestBody = request.maybeBody,
      responseStatus = status,
      responseBody = if (json == JsNull) None else Some(Json.prettyPrint(json))
    )
    val apiLog = collection.flatMap(_.insert(log))
    apiLog.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully insert apiLog with result: $writeResult")
    }
    Future(log)
  }

  def delete(id: String): Future[Unit] = Future.successful {
    val writeRes = collection.flatMap(_.remove(obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully delete token with result: $writeResult")
    }
  }

}