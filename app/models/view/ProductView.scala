package models.view

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class ProductView(
  _id: Option[BSONObjectID],
  productId: Option[BSONObjectID],
  viewer: List[Viewer]
)

object ProductView {
  implicit val viewFormat = Json.format[ProductView]
}
