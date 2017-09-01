package models.category

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json
import reactivemongo.play.json.BSONFormats._

case class Category(
  _id: Option[BSONObjectID],
  productId: List[BSONObjectID],
  categoryName: String,
  khName: String,
  categoryDescription: Option[String],
  parentId: BSONObjectID,
  ancestorId: List[BSONObjectID],
  categoryIcon: Option[String],
  commonCategory: Option[Int],
  deleted: Option[Int]
)

object Category {
  implicit val categoryFormat = Json.format[Category]
}
