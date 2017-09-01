package models.promoted

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

/**
 * Created by Ky Sona on 3/9/2017.
 */

case class PromoteProductPackage(
  _id: Option[BSONObjectID],
  typePromote: String,
  duration: Int,
  price: Double,
  description: String,
  createDate: Option[DateTime],
  status: Int
)

object PromoteProductPackage {
  implicit val promotePackageFormat = Json.format[PromoteProductPackage]
}

