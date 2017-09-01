package models.mistake

/**
 * Created by Naseat on 4/11/2017.
 */

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

case class Mistake(
  _id: Option[BSONObjectID],
  description: String,
  mistakeDate: Option[DateTime],
  deleted: Option[Int]
)

object Mistake {
  implicit val mistakeFormat = Json.format[Mistake]
}
