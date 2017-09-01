package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.promoted.{ PromoteUser, PromoteProduct }
import org.joda.time.DateTime
import reactivemongo.bson.BSONObjectID
import services.promoted.{ IPromoteUserService }
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import utils.FormatDate
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Ky Sona on 5/3/2017.
 */
class AdminManageUserPromote @Inject() (promoteUserService: IPromoteUserService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /* MEMBER REQUEST TO BE MERCHANT */
  def insertPromotedUser = SecuredApiActionWithBody { implicit request =>
    readFromRequest[PromoteUser] {
      case promote => {
        promoteUserService.checkMemberRequest(promote.userId.get).flatMap {
          case Some(requested) => {
            ok(Json.obj(
              "message" -> "Member request is already exist",
              "code" -> 500
            ))
          }
          case None => {
            promoteUserService.insertMemberRequest(
              PromoteUser(
                Some(BSONObjectID.generate()),
                promote.userId,
                promote.packageId,
                promote.duration,
                promote.price,
                Some(FormatDate.parseDate(FormatDate.printDate(promote.startDate.get))),
                Some(FormatDate.parseDate(FormatDate.printDate(promote.endDate.get))),
                promote.status
              )
            ).flatMap {
                case writeRes if writeRes.n > 0 =>
                  ok(Json.obj(
                    "message" -> "Successfully add new promoted request member",
                    "code" -> 200
                  ))
                case writeRes if writeRes.n < 1 =>
                  ok(Json.obj(
                    "message" -> "Fail with add new promoted request member",
                    "code" -> 500
                  ))
              }.fallbackTo(errorInternal)
          } // close case none
        }
      }
    }
  }

  /* ADMIN LIST MEMBER REQUEST */
  implicit val listRequestMemberReads: Reads[(String, String, String, String)] =
    (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] tupled
  def ListMemberRequest(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String)] {
      case (name, location, fromDate, toDate) => {
        promoteUserService.listMemberRequests(name, location, fromDate, toDate, page, limit).flatMap {
          case requested if requested nonEmpty => {
            ok(Json.obj(
              "message" -> "Member request found",
              "requested" -> requested,
              "code" -> 200
            ))
          }
          case requested if requested isEmpty => {
            ok(Json.obj(
              "message" -> "Member request not found",
              "code" -> 500
            ))
          }
        }.fallbackTo(errorInternal)
      } // close case
    }
  }

  /* ADMIN PROMOTES MEMBER TO BE MERCHANT */
  implicit val promoteMemberReads: Reads[(String, String, String, String, String)] =
    (__ \ "id").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "userType").read[String] and
      (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] tupled
  def promoteMember = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String, String)] {
      case (id, userId, userType, startDate, endDate) =>
        promoteUserService.promoteMemberById(id, userId, userType, startDate, endDate).flatMap {
          case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful update member type !!", "code" -> 200))
          case writeRes if writeRes.n < 1 => ok(Json.obj(
            "message" -> "Fail to update member type !!",
            "code" -> ERROR_BADREQUEST
          ))
        }.fallbackTo(errorInternal)
    }
  }

  /* ADMIN DELETE MEMBER REQUEST */
  def deleteMemberRequest(id: String) = SecuredApiAction { implicit request =>
    promoteUserService.deleteMemberRequest(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully delete member request",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with delete member request.",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN FILTER USERS EXPIRED */
  def listMerchantExpired(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    promoteUserService.listMerchantExpired(page, limit).flatMap {
      case users if users.nonEmpty =>
        ok(Json.obj(
          "message" -> "Merchants expired found",
          "code" -> 200,
          "users" -> users
        ))
      case users if users.isEmpty =>
        ok(Json.obj(
          "message" -> "Merchants expired not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN UPDATE USERS EXPIRED */
  def updateExpiredMerchants = SecuredApiAction { implicit request =>
    promoteUserService.updateExpiredMerchants().flatMap {
      case merchants if merchants.nonEmpty =>
        ok(Json.obj(
          "message" -> "Successfully update expired merchants",
          "code" -> 200
        ))
      case merchants if merchants.isEmpty =>
        ok(Json.obj(
          "message" -> "Fail with update expired merchants",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN LISTS MEMBER REQUEST EXPIRED */
  def listMemberRequestsExpired(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    promoteUserService.listMemberRequestsExpired(page, limit).flatMap {
      case users if users.nonEmpty =>
        ok(Json.obj(
          "message" -> "Successfully get all member requests expired",
          "code" -> 200,
          "users" -> users
        ))
      case users if users.isEmpty =>
        ok(Json.obj(
          "message" -> "Fail with get all member requests expired",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN DELETE MEMBER REQUEST EXPIRED */
  def deleteMemberRequestsExpired = SecuredApiAction { implicit request =>
    promoteUserService.deleteMemberRequestExpired().flatMap {
      case users if users.nonEmpty =>
        ok(Json.obj(
          "message" -> "Successfully deleted member requests expired",
          "code" -> 200
        ))
      case users if users.isEmpty =>
        ok(Json.obj(
          "message" -> "Fail with deleted member requests expired",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* GET MERCHANT EXPIRED */
  def checkMerchantExpired(userId: String) = SecuredApiAction { implicit request =>
    promoteUserService.checkMerchantExpired(userId).flatMap {
      case Some(merchant) =>
        ok(Json.obj(
          "message" -> "Successfully get merchant promoted",
          "code" -> 200,
          "merchant" -> Json.obj(
            "startDate" -> merchant.startDate,
            "endDate" -> merchant.endDate
          )
        ))
      case None =>
        ok(Json.obj(
          "message" -> "Fail with get merchant promoted",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

}
