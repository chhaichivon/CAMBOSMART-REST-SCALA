package models.advertisement

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class CategoryAdvertisement(
  _id: Option[BSONObjectID],
  categoryId: Option[BSONObjectID],
  name: String,
  description: String,
  price: Double,
  advertise: Option[List[Advertise]]
)

object CategoryAdvertisement {
  implicit val categoryAdvertisementFormat = Json.format[CategoryAdvertisement]
}
