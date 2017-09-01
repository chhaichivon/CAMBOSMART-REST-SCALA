package models.notification

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._

case class Notifications(
  _id: Option[BSONObjectID],
  userId: BSONObjectID,
  notificationType: String,
  description: String,
  isView: Boolean,
  isDirty: Boolean,
  date: Option[DateTime]
)

object Notifications {
  implicit val notificationsFormat = Json.format[Notifications]
}