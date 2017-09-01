package models.advertisement

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Advertiser(
  _id: Option[BSONObjectID],
  name: String,
  description: String,
  phones: List[String],
  email: String,
  city: String,
  address: String,
  image: String,
  url: String,
  createDate: Option[DateTime],
  status: Option[Int]
)

object Advertiser {
  implicit val advertiserFormat = Json.format[Advertiser]
}