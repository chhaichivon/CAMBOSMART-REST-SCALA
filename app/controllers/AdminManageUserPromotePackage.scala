package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.promoted.{ PromoteUserPackage }
import reactivemongo.bson.BSONObjectID
import services.promoted.{ IPromoteUserPackageService }
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Ky Sona on 4/7/2017.
 */
class AdminManageUserPromotePackage @Inject() (promoteUserPackageService: IPromoteUserPackageService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {
  /* ADD USER PACKAGE */
  implicit val saveUserPackageReads: Reads[(Int, Double, String, Int)] =
    (__ \ "duration").read[Int] and
      (__ \ "price").read[Double] and
      (__ \ "description").read[String] and
      (__ \ "status").read[Int] tupled

  def saveUserPromotePackage = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(Int, Double, String, Int)] {
      case (duration, price, description, status) => {
        promoteUserPackageService.insertUserPromotePackage(
          PromoteUserPackage(
            Some(BSONObjectID.generate()),
            duration,
            price,
            description,
            status
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully insert new promoted user package",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with insert new promoted user package",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }

  /* LIST USER PACKAGE WITH PAGINATION */
  def listUserPromotePackages(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    promoteUserPackageService.listUserPromotePackage(page, limit).flatMap {
      case packages if packages.nonEmpty =>
        ok(Json.obj(
          "message" -> "User packages found",
          "code" -> 200,
          "packages" -> packages
        ))
      case packages if packages.isEmpty =>
        ok(Json.obj(
          "message" -> "User packages not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)

  }

  /* LIST ALL DURATION USER PACKAGES */
  def listAllUserDurationPackages = ApiAction { implicit request =>
    promoteUserPackageService.listAllUserPromotePackages().flatMap {
      case packages if packages.nonEmpty =>
        ok(Json.obj(
          "message" -> "Durations found",
          "code" -> 200,
          "packages" -> packages
        ))
      case packages if packages.isEmpty =>
        ok(Json.obj(
          "message" -> "Durations not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* GET PROMOTE USER PACKAGE BY ID */
  def getUserPromotePackagesById(id: String) = ApiAction { implicit request =>
    promoteUserPackageService.getUserPromotePackageById(id).flatMap {
      case Some(packages) =>
        ok(Json.obj(
          "message" -> "Promoted user package found",
          "code" -> 200,
          "packages" -> packages
        ))
      case None =>
        ok(Json.obj(
          "message" -> "Promoted user package not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* UPDATE USER PACKAGE */
  implicit val updateUserPackageReads: Reads[(String, Int, Double, String, Int)] =
    (__ \ "_id").read[String] and
      (__ \ "duration").read[Int] and
      (__ \ "price").read[Double] and
      (__ \ "description").read[String] and
      (__ \ "status").read[Int] tupled

  def updatePromotePackage = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Int, Double, String, Int)] {
      case (id, duration, price, description, status) => {
        promoteUserPackageService.updateUserPromotePackageById(
          PromoteUserPackage(
            Some(BSONObjectID.parse(id).get),
            duration,
            price,
            description,
            status
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully updated promoted user package",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with updated promoted user package",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }

  /* DELETE PROMOTE USER PACKAGE */
  def deletePromotePackages(id: String) = SecuredApiAction { implicit request =>
    promoteUserPackageService.deleteUserPromotePackageById(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully deleted promoted user package",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with deleted promoted user package",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

}
