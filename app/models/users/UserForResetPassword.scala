package models.users

import play.api.libs.json.Json

/**
 * Created by Ky Sona on 1/18/2017.
 */
case class UserForResetPassword(
  password: String,
  verifiedCode: String
)

object UserForResetPassword {
  implicit val userEmailFormat = Json.format[UserForResetPassword]
}

