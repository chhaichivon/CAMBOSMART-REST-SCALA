package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.users.User
import play.api.i18n.MessagesApi
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import services.User.IUserService
import play.api.libs.functional.syntax._
import play.api.mvc.Action
import utils.ImageBase64
import reactivemongo.play.json.BSONFormats._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class AdminManageMerchant @Inject() (userService: IUserService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  implicit val filterMerchantReads: Reads[(String, String, String, Int, String, String)] =
    (__ \ "userType").read[String] and
      (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String]tupled

  def listFilterMerchants(start: Int, limit: Int) = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, Int, String, String)] {
      case (userType, name, location, status, fromDate, toDate) =>
        userService.getFilterMembers(userType, name, location, status, fromDate, toDate, start, limit).flatMap {
          users =>
            {
              var newUsers: List[JsObject] = List()
              for (user <- users) {
                newUsers = newUsers :+
                  Json.obj(
                    "userId" -> (user \ "_id").as[BSONObjectID].stringify,
                    "mistakeId" -> (user \ "mistakeId").as[List[BSONObjectID]],
                    "userName" -> (user \ "userName").as[String],
                    "name" -> (user \ "name").as[String],
                    "phone" -> (user \ "phone").as[String],
                    "email" -> (user \ "email").as[String],
                    "otherPhones" -> (user \ "otherPhones").as[List[String]],
                    "location" -> (user \ "location").as[String],
                    "address" -> (user \ "address").as[String],
                    "userType" -> (user \ "userType").as[String],
                    "dateJoin" -> (user \ "dateJoin").as[Long],
                    "dateBlock" -> (user \ "dateBlock").as[Long],
                    "profileImage" -> ImageBase64.image64((user \ "profileImage").as[String]),
                    "status" -> (user \ "status").as[Int],
                    "online" -> (user \ "online").as[Int],
                    "total" -> (user \ "total").as[String]
                  )
              }
              ok(newUsers)
            }
        }.fallbackTo(errorInternal)
    }
  }
  def blockMerchant(id: String) = SecuredApiAction { implicit request =>
    userService.blockMerchantById(id).flatMap {
      case writeRes if writeRes.n > 0 => ok(Json.obj("Message" -> "Successful block Merchant", "code" -> 200))
      case writeRes if writeRes.n < 1 => ok(Json.obj("Message" -> "Fail to block Merchant", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }
  def viewMerchant(id: String) = SecuredApiAction { implicit request =>
    userService.getMerchantById(id).flatMap {
      case None => ok(Json.obj("message" -> "User not found", "code" -> ERROR_USER_NOTFOUND))
      case Some(user) =>
        ok(Json.obj(
          "message" -> "User found",
          "code" -> 200,
          "user" -> Json.obj(
            "userName" -> user.userName,
            "phone" -> user.phone,
            "email" -> user.email,
            "otherPhones" -> user.otherPhones.get,
            "location" -> user.city,
            "address" -> user.address,
            "dateJoin" -> user.dateJoin.get,
            "dateBlock" -> user.dateBlock.get,
            "userType" -> user.userType,
            "profileImage" -> ImageBase64.image64(user.profileImage.get),
            "status" -> user.status,
            "online" -> user.online
          )
        ))
    }.fallbackTo(errorInternal)
  }
  def viewImage(id: String) = Action { request =>
    val user: User = Await.result(userService.getMerchantById(id), 10.seconds).get
    Ok(views.html.profile(user))
  }

  def countAllMerchantMembers() = SecuredApiAction { implicit request =>
    userService.countAllMerchantMembers().flatMap(result =>
      ok(Json.obj(
        "message" -> "Merchant Members Counting found",
        "code" -> 200,
        "data" -> result
      )))
  }
}
