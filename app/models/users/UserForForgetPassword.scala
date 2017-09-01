package models.users

import play.api.libs.json.Json

case class UserForForgetPassword(
  phone: String,
  email: String
)

object UserForForgetPassword {
  implicit val userEmailFormat = Json.format[UserForForgetPassword]
}
