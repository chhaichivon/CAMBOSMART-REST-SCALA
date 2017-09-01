package models.rating

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

/**
 * Created by Sopheak.
 * Model star rating on products
 * To store ip address object rating product
 *
 */
case class StarRating(
  _id: Option[BSONObjectID],
  productId: BSONObjectID,
  raters: List[Rater]
)

case class Rater(
  ip: String,
  star: Int
)

object Rater {
  implicit val raterFormat = Json.format[Rater]
}

object StarRating {
  implicit val starRatingFormat = Json.format[StarRating]
}