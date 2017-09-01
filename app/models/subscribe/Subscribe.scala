package models.subscribe

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Subscribe(
  _id: Option[BSONObjectID],
  userId: BSONObjectID,
  storeId: BSONObjectID,
  email: String
)

object Subscribe {
  implicit val subscribeFormat = Json.format[Subscribe]
}