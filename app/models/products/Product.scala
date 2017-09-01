package models.products

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

case class Product(
  _id: Option[BSONObjectID],
  productName: String,
  productDescription: String,
  productImage: Option[String],
  createDate: Option[DateTime],
  expireDate: Option[DateTime],
  price: Double,
  discount: Double,
  deleted: Option[Int],
  tax: String,
  productCode: String,
  productHit: Option[Int]
)

object Product {
  implicit val productFormat = Json.format[Product]
}
