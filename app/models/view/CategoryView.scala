package models.view

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class CategoryView(
  _id: Option[BSONObjectID],
  categoryId: Option[BSONObjectID],
  viewer: List[Viewer],
  deleted: Option[Int]
)
object CategoryView {
  implicit val viewFormat = Json.format[CategoryView]
}

