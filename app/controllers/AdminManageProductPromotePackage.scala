package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.promoted.PromoteProductPackage
import org.joda.time.DateTime
import reactivemongo.bson.BSONObjectID
import services.promoted.{ IPromoteProductPackageService }
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import utils.FormatDate

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Ky Sona on 3/15/2017.
 */
class AdminManageProductPromotePackage @Inject() (promotePackageService: IPromoteProductPackageService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /* LIST PACKAGE WITH PAGINATION */
  def listPromotePackages(page: Int, limit: Int, typePromote: String) = SecuredApiAction { implicit request =>
    promotePackageService.listPromotePackage(typePromote, page, limit).flatMap {
      case packages if packages.nonEmpty =>
        ok(Json.obj(
          "message" -> "Packages found",
          "code" -> 200,
          "packages" -> packages
        ))
      case packages if packages.isEmpty =>
        ok(Json.obj(
          "message" -> "Packages not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)

  }

  /* ADD PACKAGE */
  implicit val savePackageReads: Reads[(String, Int, Double, String, Int)] =
    (__ \ "typePromote").read[String] and
      (__ \ "duration").read[Int] and
      (__ \ "price").read[Double] and
      (__ \ "description").read[String] and
      (__ \ "status").read[Int] tupled

  def savePromotePackage = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Int, Double, String, Int)] {
      case (typePromote, duration, price, description, status) => {
        promotePackageService.insertPromotePackage(
          PromoteProductPackage(
            Some(BSONObjectID.generate()),
            typePromote,
            duration,
            price,
            description,
            Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
            status
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully insert new promoted package",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with insert new promoted package",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }

  /* GET PROMOTE PACKAGE BY ID */
  def getPromotePackagesById(id: String) = SecuredApiAction { implicit request =>
    promotePackageService.getPromotePackageById(id).flatMap {
      case Some(packages) =>
        ok(Json.obj(
          "message" -> "Promoted package found",
          "code" -> 200,
          "packages" -> packages
        ))
      case None =>
        ok(Json.obj(
          "message" -> "Promoted package not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* UPDATE PACKAGE */
  implicit val updatePackageReads: Reads[(String, String, Int, Double, String, Int)] =
    (__ \ "_id").read[String] and
      (__ \ "typePromote").read[String] and
      (__ \ "duration").read[Int] and
      (__ \ "price").read[Double] and
      (__ \ "description").read[String] and
      (__ \ "status").read[Int] tupled

  def updatePromotePackage = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, Double, String, Int)] {
      case (id, typePromote, duration, price, description, status) => {
        promotePackageService.updatePromotePackageById(
          PromoteProductPackage(
            Some(BSONObjectID.parse(id).get),
            typePromote,
            duration,
            price,
            description,
            Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
            status
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully updated promoted package",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with updated promoted package",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }

  /* DELETE PROMOTE PACKAGE */
  def deletePromotePackages(id: String) = SecuredApiAction { implicit request =>
    promotePackageService.deletePromotePackageById(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully deleted promoted package",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with deleted promoted package",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* LIST ALL DURATION PACKAGES */
  def listAllDurationPackages = ApiAction { implicit request =>
    promotePackageService.listAllPackages().flatMap {
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

}
