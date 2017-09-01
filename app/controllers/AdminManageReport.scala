package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import reactivemongo.bson.BSONObjectID
import services.reports.{ IAdvertiserReportService, IPromoteMemberReportService, IBootProductReportService }
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Ky Sona on 4/5/2017.
 */
class AdminManageReport @Inject() (bootProductReportService: IBootProductReportService, promoteMemberReportService: IPromoteMemberReportService, advertiserReportService: IAdvertiserReportService,
    val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /* LIST GRAND TOTAL BOOT PRODUCTS INCOME */
  def listBootProductIncome(startDate: String, endDate: String) = SecuredApiAction { implicit request =>
    bootProductReportService.listBootProductIncomeGrand(startDate, endDate).flatMap {
      case income if income.nonEmpty =>
        ok(Json.obj(
          "message" -> "income found",
          "code" -> 200,
          "incomes" -> income
        ))
      case income if income.isEmpty =>
        ok(Json.obj(
          "message" -> "income not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* LIST BOOT PRODUCTS INCOME DETAIL */
  implicit val detailBootProductIncomeByUserTypeReads: Reads[(String, String, String, Int, Int)] =
    (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] and
      (__ \ "userType").read[String] and
      (__ \ "start").read[Int] and
      (__ \ "limit").read[Int] tupled
  def listBootProductIncomeByUserType = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, Int, Int)] {
      case (startDate, endDate, userType, start, limit) => {
        bootProductReportService.listBootProductIncomeDetail(startDate, endDate, userType, start, limit).flatMap {
          case income if income.nonEmpty =>
            ok(Json.obj(
              "message" -> "income found",
              "code" -> 200,
              "incomes" -> income
            ))
          case income if income.isEmpty =>
            ok(Json.obj(
              "message" -> "income not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }
  /* LIST DETAIL PROMOTE MEMBER */
  implicit val detailPromoteMemberIncomeReads: Reads[(String, String, String, String)] =
    (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] tupled

  def listDetailPromoteMemberIncome(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String)] {
      case (name, location, fromDate, toDate) => {
        promoteMemberReportService.listDetailPromoteMemberIncome(name, location, fromDate, toDate, page, limit).flatMap {
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

  /* LIST GRAND TOTAL PROMOTE MEMBER */
  implicit val grandPromoteMemberIncomeReads: Reads[(String, String)] =
    (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] tupled
  def listGrandPromoteMemberIncome = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String)] {
      case (startDate, endDate) => {
        promoteMemberReportService.listGrandTotalPromoteMemberIncome(startDate, endDate).flatMap {
          case income if income.nonEmpty =>
            ok(Json.obj(
              "message" -> "income found",
              "code" -> 200,
              "incomes" -> income
            ))
          case income if income.isEmpty =>
            ok(Json.obj(
              "message" -> "income not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* LIST DETAIL ADVERTISERS INCOME */
  implicit val detailAdvertiserIncomeReads: Reads[(String, String, String, String, String)] =
    (__ \ "page").read[String] and
      (__ \ "city").read[String] and
      (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] and
      (__ \ "name").read[String] tupled
  def listDetailAdvertiserIncome(start: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String, String)] {
      case (page, city, startDate, endDate, name) => {
        advertiserReportService.listDetailAdvertiserIncome(page, city, startDate, endDate, name, start, limit).flatMap {
          case advertisers if advertisers.nonEmpty =>
            ok(Json.obj(
              "message" -> "income found",
              "code" -> 200,
              "advertisers" -> advertisers
            ))
          case advertisers if advertisers.isEmpty =>
            ok(Json.obj(
              "message" -> "income not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* LIST GRAND ADVERTISERS INCOME */
  implicit val grandAdvertiserIncomeReads: Reads[(String, String, String)] =
    (__ \ "page").read[String] and
      (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] tupled
  def listGrandAdvertiserIncome = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String)] {
      case (page, startDate, endDate) => {
        advertiserReportService.listGrandTotalAdvertiserIncome(page, startDate, endDate).flatMap {
          case advertisers if advertisers.nonEmpty =>
            ok(Json.obj(
              "message" -> "income found",
              "code" -> 200,
              "advertisers" -> advertisers
            ))
          case advertisers if advertisers.isEmpty =>
            ok(Json.obj(
              "message" -> "income not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* LIST CATEGORY INCOME REPORT */
  def listCategoryIncome(startDate: String, endDate: String, name: String) = SecuredApiAction { implicit request =>
    advertiserReportService.listCategoryIncome(startDate, endDate, name).flatMap {
      case advertisers if advertisers.nonEmpty =>
        ok(Json.obj(
          "message" -> "income found",
          "code" -> 200,
          "advertisers" -> advertisers
        ))
      case advertisers if advertisers.isEmpty =>
        ok(Json.obj(
          "message" -> "income not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

}
