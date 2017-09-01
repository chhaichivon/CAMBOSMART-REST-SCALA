package models.users

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONObjectID }
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, JsValue, Json, Reads, Writes }
import reactivemongo.play.json.BSONFormats._

case class User(
  _id: Option[BSONObjectID],
  mistakeId: Option[List[BSONObjectID]],
  socialId: Option[String],
  userName: String,
  phone: String,
  email: String,
  password: String,
  verifiedCode: Option[String],
  otherPhones: Option[List[String]],
  city: String,
  address: Option[String],
  userType: Option[String],
  dateJoin: Option[DateTime],
  dateBlock: Option[DateTime],
  profileImage: Option[String],
  status: Option[Int],
  online: Option[Int]
)
object User {

  //  implicit val dateTimeReads: Reads[DateTime] = (JsPath \ "$date").read[Long].map { dateTime =>
  //    new DateTime(dateTime)
  //  }
  //  implicit val dateTimeWrites = new Writes[DateTime] {
  //    def writes(dt: DateTime): JsValue = {
  //      Json.toJson(BSONDateTime(dt.getMillis + 25200000))
  //    }
  //  }
  implicit val userFormat = Json.format[User]
  //
  //  val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  //  val externalDateTimeReads = play.api.libs.json.Reads.jodaDateReads(dateTimePattern)
  //  val externalDateTimeWrites = play.api.libs.json.Writes.jodaDateWrites(dateTimePattern)
  //
  //  val externalEventWrites: Writes[User] = (
  //    (JsPath \ "_id").writeNullable[BSONObjectID] and
  //    (JsPath \ "mistakeId").writeNullable[List[BSONObjectID]] and
  //    (JsPath \ "socialId").writeNullable[String] and
  //    (JsPath \ "firstName").write[String] and
  //    (JsPath \ "lastName").write[String] and
  //    (JsPath \ "phone").write[String] and
  //    (JsPath \ "email").write[String] and
  //    (JsPath \ "password").write[String] and
  //    (JsPath \ "verifiedCode").writeNullable[String] and
  //    (JsPath \ "otherPhones").writeNullable[List[String]] and
  //    (JsPath \ "city").write[String] and
  //    (JsPath \ "address").writeNullable[String] and
  //    (JsPath \ "userType").writeNullable[String] and
  //    (JsPath \ "dateJoin").writeNullable[DateTime](externalDateTimeWrites) and
  //    (JsPath \ "dateBlock").writeNullable[DateTime](externalDateTimeWrites) and
  //    (JsPath \ "profileImage").writeNullable[String] and
  //    (JsPath \ "status").writeNullable[Int] and
  //    (JsPath \ "online").writeNullable[Int]
  //  )(unlift(User.unapply))
  //
  //  val externalEventRead: Reads[User] = (
  //    (JsPath \ "_id").readNullable[BSONObjectID] and
  //    (JsPath \ "mistakeId").readNullable[List[BSONObjectID]] and
  //    (JsPath \ "socialId").readNullable[String] and
  //    (JsPath \ "firstName").read[String] and
  //    (JsPath \ "lastName").read[String] and
  //    (JsPath \ "phone").read[String] and
  //    (JsPath \ "email").read[String] and
  //    (JsPath \ "password").read[String] and
  //    (JsPath \ "verifiedCode").readNullable[String] and
  //    (JsPath \ "otherPhones").readNullable[List[String]] and
  //    (JsPath \ "city").read[String] and
  //    (JsPath \ "address").readNullable[String] and
  //    (JsPath \ "userType").readNullable[String] and
  //    (JsPath \ "dateJoin").readNullable[DateTime](externalDateTimeReads) and
  //    (JsPath \ "dateBlock").readNullable[DateTime](externalDateTimeReads) and
  //    (JsPath \ "profileImage").readNullable[String] and
  //    (JsPath \ "status").readNullable[Int] and
  //    (JsPath \ "online").readNullable[Int]
  //  )(User.apply _)
}