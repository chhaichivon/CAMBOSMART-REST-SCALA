package models.view

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.play.json.BSONFormats._

case class Viewer(
  ipAddress: String,
  viewDate: Option[DateTime],
  views: Int
)

object Viewer {
  implicit val viewerFormat = Json.format[Viewer]
}
