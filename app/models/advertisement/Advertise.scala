package models.advertisement

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Advertise(
  id: Option[BSONObjectID],
  duration: Int,
  startDate: DateTime,
  expireDate: DateTime,
  price: Double,
  discount: Double
)

object Advertise {
  implicit val advertiseFormat = Json.format[Advertise]
}