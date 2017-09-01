package models.view

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import reactivemongo.play.json.BSONFormats._
import play.api.libs.functional.syntax._

case class VisitorView(
  _id: Option[BSONObjectID],
  ip: String,
  visitedDate: Option[DateTime],
  amount: Int
)

object VisitorView {

  implicit val dateTimeReads: Reads[DateTime] = (__ \ "$date").read[Long].map { dateTime =>
    new DateTime(dateTime)
  }
  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(dt: DateTime): JsValue = {
      Json.toJson(BSONDateTime(dt.getMillis + 25200000))
    }
  }
  implicit val visitorViewFormat = Json.format[VisitorView]

  val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  val externalDateTimeReads = play.api.libs.json.Reads.jodaDateReads(dateTimePattern)
  val externalDateTimeWrites = play.api.libs.json.Writes.jodaDateWrites(dateTimePattern)

  val externalEventWrites: Writes[VisitorView] = (
    (__ \ "_id").writeNullable[BSONObjectID] and
    (__ \ "ip").write[String] and
    (__ \ "visitedDate").writeNullable[DateTime](externalDateTimeWrites) and
    (__ \ "amount").write[Int]
  )(unlift(VisitorView.unapply))

  val externalEventRead: Reads[VisitorView] = (
    (__ \ "_id").readNullable[BSONObjectID] and
    (__ \ "ip").read[String] and
    (__ \ "visitedDate").readNullable[DateTime](externalDateTimeReads) and
    (__ \ "amount").read[Int]
  )(VisitorView.apply _)
}