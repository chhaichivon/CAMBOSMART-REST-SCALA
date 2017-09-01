package models.product

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._
case class Product(
  _id: Option[BSONObjectID],
  productName: String,
  productDescription: String,
  productImage: Option[List[String]],
  price: Double,
  discount: Option[Double],
  discountFromDate: Option[DateTime],
  discountEndDate: Option[DateTime],
  productType: Option[String],
  createDate: Option[DateTime],
  expireDate: Option[DateTime],
  status: Int,
  blockDate: Option[DateTime]
)

object Product {
  implicit val productFormat = Json.format[Product]
}
