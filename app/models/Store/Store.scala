package models.store

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json
import reactivemongo.play.json.BSONFormats._

case class Store(
  _id: Option[BSONObjectID],
  userId: Option[BSONObjectID],
  productId: Option[List[BSONObjectID]],
  storeName: String,
  storeInformation: String,
  storeBanner: Option[String],
  storeUrl: Option[List[String]],
  latitude: Option[Double],
  longitude: Option[Double]
)

object Store {
  implicit val storeFormat = Json.format[Store]
}
