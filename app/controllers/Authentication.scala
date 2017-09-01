package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.ApiToken
import models.store.Store
import models.users._
import utils.SMS.SendSMS
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.mailer.{ Email, MailerClient }
import reactivemongo.bson.BSONObjectID
import services.Auth.IAuthService
import utils.{ FormatDate, ImageBase64 }

import scala.concurrent.ExecutionContext.Implicits.global

class Authentication @Inject() (authService: IAuthService, val messagesApi: MessagesApi, system: ActorSystem, mailer: MailerClient) extends api.ApiController {

  /**
   * Function socialSignUp use to register account
   * with social information in www.cambosmart.com
   *
   * @return JSON object result
   */
  def socialSignUp = ApiActionWithBody { implicit request =>
    readFromRequest[User] { user =>
      authService.getUserBySocialId(user.socialId.get).flatMap {
        case Some(usr) =>
          if (usr.phone != "" && usr.status.get != 0) {
            ApiToken.create(request.apiKeyOpt.get, usr._id.get.stringify).flatMap { token =>
              {
                ok(Json.obj(
                  "message" -> "Successfully login with www.cambosmart.com",
                  "code" -> 200,
                  "user" -> Json.obj(
                    "userId" -> usr._id.get.stringify,
                    "userName" -> usr.userName,
                    "phone" -> usr.phone,
                    "email" -> usr.email,
                    "otherPhones" -> usr.otherPhones,
                    "location" -> usr.city,
                    "address" -> usr.address,
                    "userType" -> usr.userType,
                    "profileImage" -> user.profileImage.get,
                    "status" -> usr.status,
                    "online" -> usr.online
                  ),
                  "token" -> token,
                  "minutes" -> 1440 // token life is 1 week
                ))
              }
            }
          } else if (usr.phone != "" && usr.status.get == 0) {
            ok(Json.obj(
              "message" -> "Account is not activated !",
              "code" -> ERROR_USER_INACTIVE,
              "user" -> Json.obj(
                "userName" -> user.userName,
                "phone" -> usr.phone,
                "userType" -> user.userType
              )
            ))
          } else if (usr.phone != "" && usr.status.get == -1) {
            ok(Json.obj(
              "message" -> "Account was blocked !",
              "code" -> ERROR_USER_DISABLE
            ))
          } else {
            ok(Json.obj("message" -> s"Phone number is invalid !", "code" -> ERROR_PHONE_NULL))
          }
        case None =>
          val id = BSONObjectID.generate()
          authService.insertUser(
            User(
              Some(id),
              Some(List()),
              user.socialId,
              user.userName,
              user.phone,
              user.email,
              user.password,
              Some(""),
              Some(List()),
              user.city,
              Some(""),
              Some("normal"),
              Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
              Some(FormatDate.parseDate("1970-1-1")),
              Some(""),
              Some(0),
              Some(0)
            )
          ).flatMap {
              case r if r.n > 0 =>
                authService.insertStore(
                  Store(
                    Some(BSONObjectID.generate()),
                    Some(id),
                    Some(List()),
                    "",
                    //user.city,
                    "",
                    Some(""),
                    Some(List()),
                    Some(11.5448729),
                    Some(104.8921668)
                  )
                ).flatMap {
                    case result if result.n > 0 =>
                      ok(Json.obj(
                        "message" -> "Successful add new user by social account !!",
                        "code" -> 201,
                        "user" -> Json.obj(
                          "userName" -> user.userName,
                          "phone" -> user.phone,
                          "userType" -> "normal"
                        )
                      ))
                    case result if result.n < 1 =>
                      authService.deleteUserById(id.stringify)
                      ok(Json.obj(
                        "message" -> "Fail to add new user !!",
                        "code" -> ERROR_BADREQUEST
                      ))
                  }
              case r if r.n < 1 =>
                ok(Json.obj(
                  "message" -> "Fail to add new user !!",
                  "code" -> ERROR_BADREQUEST
                ))
            }
      }
    }
  }

  implicit val socialPhoneReads: Reads[(String, String, Option[String])] =
    (__ \ "socialId").read[String] and
      (__ \ "phone").read[String] and
      (__ \ "other").readNullable[String] tupled

  def socialPhoneUpdate = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Option[String])] {
      case (socialId, phone, other) =>
        authService.getUserBySocialId(socialId).flatMap {
          case Some(user) =>
            authService.getUserByPhone(phone).flatMap {
              case Some(data) => ok(Json.obj("message" -> s"Phone number ${data.phone.replace("+855", "0")} is already used, please try another !!", "code" -> ERROR_USER_EXIST))
              case None =>
                val code = authService.randomCode()
                authService.updateUserBySocialId(socialId, phone, code).flatMap { result =>
                  try {
                    SendSMS(phone, code)
                    ok(Json.obj(
                      "message" -> "Successful add phone number to www.cambosmart.com social user account !",
                      "code" -> 200,
                      "user" -> Json.obj(
                        "userName" -> user.userName,
                        "phone" -> phone,
                        "userType" -> user.userType
                      )
                    ))
                  } catch {
                    case e: Throwable =>
                      e.printStackTrace()
                      ok(Json.obj(
                        "message" -> s"Can not send message to your phone number $phone !",
                        "code" -> 200,
                        "user" -> Json.obj(
                          "userName" -> user.userName,
                          "phone" -> phone,
                          "userType" -> user.userType
                        )
                      ))
                  }
                }
            }
          case None =>
            ok(Json.obj("message" -> s"socialId $socialId is invalid.", "code" -> ERROR_USER_NOTFOUND))
        }.fallbackTo(errorInternal)
    }

  }

  /**
   * Function personalSignUp use to register account
   * with personal information in www.cambosmart.com
   *
   * @return JSON object result
   */
  def personalSignUp = ApiActionWithBody { implicit request =>
    readFromRequest[User] { user =>
      val id = BSONObjectID.generate()
      val code = authService.randomCode()
      val mailContent = Email(
        "Welcome to CAMBOSMART website - User account activation",
        "caoruth1013@gmail.com",
        Seq(user.email.toString),
        bodyText = Some("activate user account"),
        bodyHtml = Some("<html>" +
          "<body>" +
          "<span>Dear " + user.userName.toString + ", <br/><br/> " +
          "Thank you to register in CAMBOSMART.<br/>" +
          "If this is not you just ignore this email.<br/><br/>" +
          "<h1> YOUR CODE : " + code + "</h1><br/>" +
          "Warm Regard,<br/><br/>" +
          "CAMBOSMART Team" +
          "</span>" +
          "</body>" +
          "</html>")
      )
      authService.getUserByName(user.userName).flatMap {
        case data if data.isEmpty =>
          if (user.phone != "") {
            //use phone number to register with www.cambosmart.com
            authService.getUserByPhone(user.phone).flatMap {
              case Some(usr) =>
                if (usr.status.get == 0) {
                  ok(Json.obj("message" -> s"Phone number ${usr.phone.replace("+855", "0")} is already used, but unverified !", "code" -> ERROR_USER_INACTIVE))
                } else if (usr.status.get < 0) {
                  ok(Json.obj("message" -> s"Phone number ${usr.phone.replace("+855", "0")} is already used, but was blocked !", "code" -> ERROR_USER_DISABLE))
                } else {
                  ok(Json.obj("message" -> s"Phone number ${usr.phone.replace("+855", "0")} is already used !", "code" -> ERROR_USER_EXIST))
                }
              case None =>
                authService.insertUser(
                  User(
                    Some(id),
                    Some(List()),
                    user.socialId,
                    user.userName,
                    user.phone,
                    user.email,
                    BCrypt.hashpw(user.password, BCrypt.gensalt()),
                    Some(code),
                    Some(List()),
                    user.city,
                    Some(""),
                    Some("normal"),
                    Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
                    Some(FormatDate.parseDate("1970-1-1")),
                    Some(""),
                    Some(0),
                    Some(0)
                  )
                ).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      try {
                        SendSMS(user.phone, code)
                        authService.insertStore(
                          Store(
                            Some(BSONObjectID.generate()),
                            Some(id),
                            Some(List()),
                            "",
                            "",
                            Some(""),
                            Some(List()),
                            Some(0),
                            Some(0)
                          )
                        ).flatMap {
                            case result if result.n > 0 =>
                              ok(Json.obj(
                                "message" -> "Successful add new user by personal info !!",
                                "code" -> 200,
                                "user" -> Json.obj(
                                  "userName" -> user.userName,
                                  "phone" -> user.phone,
                                  "userType" -> "normal"
                                )
                              ))
                            case result if result.n < 1 =>
                              authService.deleteUserById(id.stringify)
                              ok(Json.obj(
                                "message" -> "Fail to add new user !!",
                                "code" -> ERROR_BADREQUEST
                              ))
                          }
                      } catch {
                        case e: Throwable =>
                          e.printStackTrace()
                          ok(Json.obj(
                            "message" -> s"Can not send message to your phone number ${user.phone.replace("+855", "0")}.",
                            "code" -> 200,
                            "user" -> Json.obj(
                              "userName" -> user.userName,
                              "phone" -> user.phone,
                              "userType" -> "normal"
                            )
                          ))
                      }
                    case writeResult if writeResult.n < 1 =>
                      ok(Json.obj(
                        "message" -> "Fail to add new user !!",
                        "code" -> ERROR_BADREQUEST
                      ))
                  }
            }
          } else {
            //use email to register with www.cambosmart.com
            authService.getUserByEmail(user.email).flatMap {
              case Some(usr) =>
                if (usr.status.get == 0) {
                  ok(Json.obj("message" -> s"Email ${usr.email} is already used, but unverified !", "code" -> ERROR_USER_INACTIVE))
                } else if (usr.status.get < 0) {
                  ok(Json.obj("message" -> s"Email ${usr.email} is already used, but was blocked !", "code" -> ERROR_USER_DISABLE))
                } else {
                  ok(Json.obj("message" -> s"Email ${usr.email} is already used !", "code" -> ERROR_USER_EXIST))
                }
              case None =>
                authService.insertUser(
                  User(
                    Some(id),
                    Some(List()),
                    user.socialId,
                    user.userName,
                    user.phone,
                    user.email,
                    BCrypt.hashpw(user.password, BCrypt.gensalt()),
                    Some(code),
                    Some(List()),
                    user.city,
                    Some(""),
                    Some("normal"),
                    Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
                    Some(FormatDate.parseDate("1970-1-1")),
                    Some(""),
                    Some(0),
                    Some(0)
                  )
                ).flatMap {
                    case r if r.n > 0 =>
                      mailer.send(mailContent)
                      authService.insertStore(
                        Store(
                          Some(BSONObjectID.generate()),
                          Some(id),
                          Some(List()),
                          "",
                          "",
                          Some(""),
                          Some(List()),
                          Some(0),
                          Some(0)
                        )
                      ).flatMap {
                          case result if result.n > 0 =>
                            ok(Json.obj(
                              "message" -> "Successful add new user by personal info !!",
                              "code" -> 200,
                              "user" -> Json.obj(
                                "userName" -> user.userName,
                                "phone" -> user.phone,
                                "userType" -> "normal"
                              )
                            ))
                          case result if result.n < 1 =>
                            authService.deleteUserById(id.stringify)
                            ok(Json.obj(
                              "message" -> "Fail to add new user account!!",
                              "code" -> ERROR_BADREQUEST
                            ))
                        }
                    case r if r.n < 1 =>
                      ok(Json.obj(
                        "message" -> "Fail to add new user account !!",
                        "code" -> ERROR_BADREQUEST
                      ))
                  }
            }
          }
        case data if data.nonEmpty =>
          ok(Json.obj("message" -> "Username is already used !!", "code" -> ERROR_USER_EXIST))
      }
    }
  }

  /**
   * Function accountVerification use to verify account with security code.
   *
   * @param code
   * @return JSON object result
   */
  def accountVerification(code: String) = ApiAction { implicit request =>
    authService.getUserByCode(code).flatMap {
      case Some(user) =>
        if (user.status.get == 1) {
          ok(Json.obj("message" -> s"Code $code is already used !", "code" -> ERROR_USER_EXIST))
        } else {
          authService.activeUserByCode(code).flatMap {
            case writeRes if writeRes.n > 0 =>
              ApiToken.create(request.apiKeyOpt.get, user._id.get.stringify).flatMap { token =>
                {
                  ok(Json.obj(
                    "message" -> "Successfully login with www.cambosmart.com",
                    "code" -> 200,
                    "user" -> Json.obj(
                      "userId" -> user._id.get.stringify,
                      "userName" -> user.userName,
                      "phone" -> user.phone,
                      "email" -> user.email,
                      "otherPhones" -> user.otherPhones,
                      "location" -> user.city,
                      "address" -> user.address,
                      "userType" -> user.userType,
                      "profileImage" -> user.profileImage.get,
                      "status" -> user.status,
                      "online" -> user.online
                    ),
                    "token" -> token,
                    "minutes" -> 1440 // token life is 1 week
                  ))
                }
              }
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj("message" -> s"Code $code is invalid !", "code" -> ERROR_BADREQUEST))
          }.fallbackTo(errorInternal)
        }
      case None =>
        ok(Json.obj("message" -> s"Code $code is invalid !", "code" -> ERROR_USER_NOTFOUND))
    }
  }

  /**
   * Function for users log in into www.cambosmart.com
   * Users can login by phone number or email
   * Return some user information for user after login success or login fail
   *
   * @return JSON object result
   */
  def signIn = ApiActionWithBody { implicit request =>
    readFromRequest[UserForSignIn] { userSignInInfo =>
      {
        // Check weather it is phone number or email
        if (userSignInInfo.phone != "") {
          // Login with phone number
          authService.getUserByPhone(userSignInInfo.phone).flatMap {
            case None => ok(Json.obj("message" -> "User not found !", "code" -> ERROR_USER_NOTFOUND))
            case Some(user) =>
              if (user.password == "") ok(Json.obj("message" -> "User not found", "code" -> ERROR_USER_NOTFOUND))
              else if (!BCrypt.checkpw(userSignInInfo.password, user.password)) ok(Json.obj("message" -> "Password is incorrect !", "code" -> ERROR_UNAUTHORIZED))
              else if (user.status.get == 0) ok(Json.obj("message" -> "Account is not activated !", "code" -> ERROR_USER_INACTIVE))
              //else if (user.status.get == -1) ok(Json.obj("message" -> "Account was block !", "code" -> ERROR_USER_DISABLE))
              else ApiToken.create(request.apiKeyOpt.get, user._id.get.stringify).flatMap { token =>
                {
                  ok(Json.obj(
                    "message" -> "Successfully login with www.cambosmart.com",
                    "code" -> 200,
                    "user" -> Json.obj(
                      "userId" -> user._id.get.stringify,
                      "userName" -> user.userName,
                      "phone" -> user.phone,
                      "email" -> user.email,
                      "otherPhones" -> user.otherPhones,
                      "otherPhones" -> user.otherPhones,
                      "location" -> user.city,
                      "address" -> user.address,
                      "userType" -> user.userType,
                      "profileImage" -> user.profileImage.get,
                      "status" -> user.status,
                      "online" -> user.online
                    ),
                    "token" -> token,
                    "minutes" -> 1440 // token life is 1 week
                  ))
                }
              }
          }
        } else {
          // Login with email
          authService.getUserByEmail(userSignInInfo.email).flatMap {
            case None => ok(Json.obj("message" -> "User not found !", "code" -> ERROR_USER_NOTFOUND))
            case Some(user) =>
              if (user.password == "") ok(Json.obj("message" -> "User not found", "code" -> ERROR_USER_NOTFOUND))
              else if (!BCrypt.checkpw(userSignInInfo.password, user.password)) ok(Json.obj("message" -> "Password is incorrect !", "code" -> ERROR_UNAUTHORIZED))
              else if (user.status.get == 0) ok(Json.obj("message" -> "Account is not activated !", "code" -> ERROR_USER_INACTIVE))
              //else if (user.status.get == -1) ok(Json.obj("message" -> "Account was block !", "code" -> ERROR_USER_DISABLE))
              else ApiToken.create(request.apiKeyOpt.get, user._id.get.stringify).flatMap { token =>
                {
                  ok(Json.obj(
                    "message" -> "Successfully login with www.cambosmart.com",
                    "code" -> 200,
                    "user" -> Json.obj(
                      "userId" -> user._id.get.stringify,
                      "userName" -> user.userName,
                      "phone" -> user.phone,
                      "email" -> user.email,
                      "otherPhones" -> user.otherPhones,
                      "location" -> user.city,
                      "address" -> user.address,
                      "userType" -> user.userType,
                      "profileImage" -> user.profileImage.get,
                      "status" -> user.status,
                      "online" -> user.online
                    ),
                    "token" -> token,
                    "minutes" -> 1440 // token life is 1 week
                  ))
                }
              }
          }
        }
      } // close else
    } // close read from form request
  }

  /**
   * Function for user forget password
   * Return verified code after enter phone number or email successfully
   * Return fail if phone number or email cannot found
   *
   * @return JSON object result
   */
  def forgetPassword = ApiActionWithBody { implicit request =>
    readFromRequest[UserForForgetPassword] {
      userForgetPassword =>
        {
          if (userForgetPassword.phone != "") {
            // forget password by phone number
            authService.getUserByPhone(userForgetPassword.phone).flatMap {
              case None => ok(Json.obj("message" -> "User not found !", "code" -> ERROR_USER_NOTFOUND)) // 109
              case Some(user) => {
                if (user.status.get == 0) {
                  ok(Json.obj("message" -> "Account is not activated !", "code" -> ERROR_USER_INACTIVE)) // 111
                } else if (user.password == "") {
                  // social user cannot forget password
                  ok(Json.obj("message" -> "User not found !", "code" -> ERROR_USER_NOTFOUND)) // 109
                } else {
                  val code = authService.randomCode() // create a new verified code for user
                  authService.updateCodeByPhone(code, user.phone).flatMap {
                    case writeRes if writeRes.n > 0 =>
                      // send verify code to user
                      SendSMS(user.phone, code)
                      ok(Json.obj(
                        "message" -> "Successfully send verify code to your phone number !",
                        "code" -> 200,
                        "user" -> Json.obj(
                          "phone" -> user.phone,
                          "verifiedCode" -> code
                        )
                      ))
                    case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with update verified code by phone !", "code" -> ERROR_BADREQUEST)) // 400
                  }.fallbackTo(errorInternal)
                }
              } // close case some
            }
          } else {
            // forget password by email
            authService.getUserByEmail(userForgetPassword.email).flatMap {
              case None => ok(Json.obj("message" -> "User not found !", "code" -> 401))
              case Some(user) => {
                if (user.status.get == 0) {
                  ok(Json.obj("message" -> "Account is not activated !", "code" -> ERROR_USER_INACTIVE)) // 111
                } else if (user.password == "") {
                  // social user cannot forget password
                  ok(Json.obj("message" -> "User not found !", "code" -> 401))
                } else {
                  val code = authService.randomCode() // generate a new verified code and save it in DB
                  authService.updateCodeByEmail(code, user.email).flatMap {
                    case writeRes if writeRes.n > 0 =>
                      val mailContent = Email(
                        subject = Messages("Reset New Password"),
                        from = Messages("caoruth1013@gmail.com"),
                        to = Seq(user.email),
                        bodyText = Some("This verified code number is used for set your new password."),
                        bodyHtml = Some("<html><body><h3>Here is your Verified Code:</h3><h1>" + code + "</h1></body></html>")
                      )
                      // send mail to user's email
                      mailer.send(mailContent)
                      ok(Json.obj(
                        "message" -> "Mail has been already sent !",
                        "code" -> 200,
                        "user" -> Json.obj(
                          "email" -> user.email,
                          "verifiedCode" -> code
                        )
                      ))
                    case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> s"Code $code is invalid !", "code" -> ERROR_NOTFOUND))
                  }.fallbackTo(errorInternal)
                }
              } // close case some
            }
          }
        }
    }
  }

  /**
   * Function for reset user password by verified code
   *
   * @return JSON object result
   */
  def resetPassword = ApiActionWithBody { implicit request =>
    readFromRequest[UserForResetPassword] { userPassword =>
      {
        authService.getUserByCode(userPassword.verifiedCode).flatMap {
          case None => ok(Json.obj("message" -> "User not found !", "code" -> 400)) //109
          case Some(user) => {
            val passwordHash = BCrypt.hashpw(userPassword.password, BCrypt.gensalt())
            authService.updatePasswordByCode(passwordHash, userPassword.verifiedCode).flatMap {
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with update user password !", "code" -> ERROR_BADREQUEST))
              case writeRes if writeRes.n > 0 =>
                ok(Json.obj(
                  "message" -> "Successfully update user password !",
                  "code" -> 200
                ))
            }
          } // close case some
        }
      }
    } // close read from form request
  }

  /**
   * Function signOut use to log out from www.cambosmart.com
   *
   * @return
   */
  def signOut = SecuredApiAction { implicit request =>
    ApiToken.delete(request.token).flatMap { _ =>
      ok(Json.obj("message" -> "Successful sign out from www.cambosmart.com", "code" -> 200))
    }
  }

  def listAllPhonesAndEmails = ApiAction { implicit request =>
    authService.getAllPhoneOrEmail.flatMap {
      users => ok(users)
    }.fallbackTo(errorInternal)
  }

  implicit val codeInfoReads: Reads[(String, String)] =
    (__ \ "phone").read[String] and
      (__ \ "email").read[String] tupled

  def sendCode = ApiActionWithBody { implicit request =>
    {
      readFromRequest[(String, String)] {
        case (phone, email) =>
          val code = authService.randomCode()
          val mailContent = Email(
            "Welcome to CAMBOSMART Website !!!",
            "caoruth1013@gmail.com",
            Seq(email.toString),
            bodyText = Some("Resend Code"),
            bodyHtml = Some("<html>" +
              "<body>" +
              "<h1> YOUR CODE : " + code + "</h1><br/>" +
              "Warm Regard,<br/><br/>" +
              "CAMBOSMART Team" +
              "</span>" +
              "</body>" +
              "</html>")
          )
          if (phone != "" && email == "") {
            authService.updateCodeByPhone(code, phone).flatMap {
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update code by phone !", "code" -> ERROR_BADREQUEST))
              case writeRes if writeRes.n > 0 =>
                SendSMS(phone, code)
                ok(Json.obj(
                  "message" -> "Successful send code by phone !",
                  "code" -> 200
                ))
            }
          } else if (email != "" && phone == "") {
            authService.updateCodeByEmail(code, email).flatMap {
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update code by email !", "code" -> ERROR_BADREQUEST))
              case writeRes if writeRes.n > 0 =>
                mailer.send(mailContent)
                ok(Json.obj(
                  "message" -> "Successful send code by email!",
                  "code" -> 200
                ))
            }
          } else if (phone != "" && email != "") {
            authService.updateCodeByPhone(code, phone).flatMap {
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update code by phone !", "code" -> ERROR_BADREQUEST))
              case writeRes if writeRes.n > 0 =>
                SendSMS(phone, code)
                ok(Json.obj(
                  "message" -> "Successful send code by phone !",
                  "code" -> 200
                ))
            }
          } else {
            ok(Json.obj(
              "message" -> "Fail to update code !",
              "code" -> ERROR_BADREQUEST
            ))
          }
      }
    }
  }

}

