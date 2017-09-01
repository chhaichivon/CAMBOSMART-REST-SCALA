package models.advertisement

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Advertisement(
  _id: Option[BSONObjectID],
  page: String,
  location: String,
  description: String,
  price: Double,
  advertise: Option[List[Advertise]]
)

object Advertisement {
  implicit val advertisementFormat = Json.format[Advertisement]
}
