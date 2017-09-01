package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.promoted.PromoteUser
import services.User.IUserService
import org.mindrot.jbcrypt.BCrypt
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.mailer.MailerClient
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import utils.{ FormatDate, ImageBase64 }
import reactivemongo.play.json.BSONFormats._

import scala.util.Random

class AdminManageMember @Inject() (userService: IUserService, val messagesApi: MessagesApi, system: ActorSystem, mailer: MailerClient) extends api.ApiController {

  /**
   * Function memberDetail use to get member by id
   *
   * @param _id
   * @return Json object of member
   */
  def memberDetail(_id: String) = ApiAction { implicit request =>
    userService.getMemberById(_id).flatMap {
      case None => ok(Json.obj("message" -> "User not found", "code" -> ERROR_USER_NOTFOUND))
      case Some(user) =>
        ok(Json.obj("message" -> "User found !!", "code" -> 200, "user" -> user))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN BLOCK MEMBER */
  implicit val blockMemberReads: Reads[(String, Int)] =
    (__ \ "_id").read[String] and
      (__ \ "status").read[Int] tupled
  def blockMember = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Int)] {
      case (_id, status) =>
        userService.blockMemberById(_id, status).flatMap {
          case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful updated member's status !!", "code" -> 200))
          case writeRes if writeRes.n < 1 => ok(Json.obj(
            "message" -> "Fail updated member's status !!",
            "code" -> ERROR_BADREQUEST
          ))
        }.fallbackTo(errorInternal)
    }
  }

  implicit val filterMemberReads: Reads[(String, String, String, Int, String, String)] =
    (__ \ "userType").read[String] and
      (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String]tupled

  def listFilterMembers(start: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, Int, String, String)] {
      case (userType, name, location, status, fromDate, toDate) =>
        userService.getFilterMembers(userType, name, location, status, fromDate, toDate, start, limit).flatMap { users =>
          ok(Json.obj("message" -> "Successful get all users !!", "code" -> 200, "users" -> users))
        }.fallbackTo(errorInternal)
    }
  }

  def uploadImage(id: String) = SecuredApiActionWithBodyFile { request =>
    request.body.file("file").map { file =>
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val filename = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      //val contentType = file.contentType
      file.ref.moveTo(new File(ImageBase64.profilePath + filename))
      userService.getMemberById(id).flatMap {
        case None => ok(Json.obj("message" -> "User not found !!", "code" -> ERROR_USER_NOTFOUND))
        case Some(user) =>
          if (user.profileImage.get != "") {
            userService.uploadImage(id, filename).flatMap {
              case writeRes if writeRes.n > 0 =>
                ImageBase64.removeFile(ImageBase64.profilePath + user.profileImage.get)
                ok(Json.obj("message" -> "Success save image !!", "profileImage" -> filename, "code" -> 200))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          } else {
            userService.uploadImage(id, filename).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Success save image !!", "profileImage" -> filename, "code" -> 200))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          }
      }
    }.getOrElse {
      ok(Json.obj("message" -> "Fail upload image !!", "code" -> ERROR_BADREQUEST))
    }
  }

  implicit val updateMemberProfileReads: Reads[(String, String, String, String, List[String], String, String)] =
    (__ \ "_id").read[String] and
      (__ \ "userName").read[String] and
      (__ \ "phone").read[String] and
      (__ \ "email").read[String] and
      (__ \ "phones").read[List[String]] and
      (__ \ "location").read[String] and
      (__ \ "address").read[String]tupled

  def updateMemberProfile() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String, List[String], String, String)] {
      case (id, userName, phone, email, phones, location, address) =>
        userService.getMemberById(id).flatMap {
          case None =>
            ok(Json.obj(
              "message" -> "User not found !",
              "code" -> ERROR_USER_NOTFOUND
            ))
          case Some(user) =>
            var newPhones = user.otherPhones.get
            if (user.otherPhones.get.nonEmpty && phones.length == 1) {
              if (user.otherPhones.get.contains(phones.head)) {
                userService.updateMemberProfile(id, userName, phone, email, phones, location, address).flatMap {
                  case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update user !!", "code" -> ERROR_BADREQUEST))
                  case writeRes if writeRes.n > 0 => ok(Json.obj(
                    "message" -> "Successful update user !!",
                    "code" -> 200
                  ))
                }.fallbackTo(errorInternal)
              } else {
                newPhones = newPhones :+ phones.head
                userService.updateMemberProfile(id, userName, phone, email, newPhones, location, address).flatMap {
                  case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update user !!", "code" -> ERROR_BADREQUEST))
                  case writeRes if writeRes.n > 0 => ok(Json.obj(
                    "message" -> "Successful update user !!",
                    "code" -> 200
                  ))
                }.fallbackTo(errorInternal)
              }
            } else {
              userService.updateMemberProfile(id, userName, phone, email, phones, location, address).flatMap {
                case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update user !!", "code" -> ERROR_BADREQUEST))
                case writeRes if writeRes.n > 0 => ok(Json.obj(
                  "message" -> "Successful update user !!",
                  "code" -> 200
                ))
              }.fallbackTo(errorInternal)
            }
        }
    }
  }

  implicit val changePasswordReads: Reads[(String, String, String)] =
    (__ \ "_id").read[String] and
      (__ \ "oldPassword").read[String] and
      (__ \ "newPassword").read[String]tupled

  /**
   * Function changePassword use to change member's password
   *
   * @return Json object
   */
  def changePassword() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String)] {
      case (id, oldPassword, newPassword) =>
        userService.getMemberById(id).flatMap {
          case None =>
            ok(Json.obj(
              "message" -> "User not found !!",
              "code" -> ERROR_USER_NOTFOUND
            ))
          case Some(user) =>
            if (user.password == "") {
              ok(Json.obj(
                "message" -> "Social user no need password !!",
                "code" -> ERROR_USER_EXIST
              ))
            } else {
              if (!BCrypt.checkpw(oldPassword, user.password)) ok(Json.obj("message" -> "Your old password is incorrect !!", "code" -> ERROR_NOTFOUND))
              else
                userService.changePassword(id, BCrypt.hashpw(newPassword, BCrypt.gensalt())).flatMap {
                  case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to change password !!", "code" -> ERROR_BADREQUEST))
                  case writeRes if writeRes.n > 0 => ok(Json.obj(
                    "message" -> "Successful changed password !!",
                    "code" -> 200
                  ))
                }.fallbackTo(errorInternal)
            }
        }
    }
  }

  def countAllNormalMembers() = SecuredApiAction { implicit request =>
    userService.countAllNormalMembers().flatMap(result =>
      ok(Json.obj(
        "message" -> "Normal Members Counting found",
        "code" -> 200,
        "data" -> result
      )))
  }

}
