package models.promoted

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

/**
 * Created by Ky Sona on 4/7/2017.
 */

case class PromoteUser(
  _id: Option[BSONObjectID],
  userId: Option[BSONObjectID],
  packageId: Option[BSONObjectID],
  duration: Int,
  price: Double,
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  status: Int
)

object PromoteUser {
  implicit val promoteUserFormat = Json.format[PromoteUser]
}
