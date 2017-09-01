package models.users

import play.api.libs.json.Json
import play.modules.reactivemongo.json._

/**
 * Created by Ky Sona on 1/17/2017.
 */
case class UserForSignIn(
  phone: String,
  email: String,
  password: String
)

object UserForSignIn {
  implicit val userSignInFormat = Json.format[UserForSignIn]
}
