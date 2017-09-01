package models.promoted

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

/**
 * Created by Ky Sona on 3/9/2017.
 */
case class PromoteProduct(
  _id: Option[BSONObjectID],
  userId: BSONObjectID,
  packageId: BSONObjectID,
  typePromote: String,
  duration: Int,
  price: Double,
  productId: List[BSONObjectID],
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  status: Int
)

object PromoteProduct {
  implicit val promoteProductFormat = Json.format[PromoteProduct]
}

