package models.advertiser

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Advertising(
  _id: Option[BSONObjectID],
  advertiserId: Option[List[BSONObjectID]],
  ads_name: String,
  website: Option[String],
  description: Option[String],
  page: String,
  location: String,
  price: Double,
  dateStart: Option[DateTime],
  dateEnd: Option[DateTime],
  bannerImg: Option[String],
  status: Option[Int],
  userId: Option[BSONObjectID]
)

object Advertising {
  implicit val advertisingFormat = Json.format[Advertising]
}