package controllers

import api.ApiError._
import com.google.inject.Inject
import models.view.VisitorView
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import services.view.IVisitorViewService

import scala.concurrent.ExecutionContext.Implicits.global

class VisitorViewController @Inject() (val visitorViewService: IVisitorViewService, val messagesApi: MessagesApi) extends api.ApiController {

  def insertVisitorView() = ApiAction { implicit request =>
    visitorViewService.getVisitorViews(request.remoteAddress, "1970-1-1", "1970-1-1").flatMap {
      case visitors if visitors.nonEmpty =>
        if (visitors.head.visitedDate.get.getMillis >= (new DateTime() minusHours 12).getMillis) {
          ok(Json.obj("message" -> "Today already count !!", "code" -> ERROR_USER_EXIST))
        } else {
          visitorViewService.insertVisitorView(
            VisitorView(
              Some(BSONObjectID.generate()),
              request.remoteAddress,
              Some(new DateTime()),
              1
            )
          ).flatMap {
              case r if r.n > 0 => ok(Json.obj("message" -> "Successful insert new visitor view !!", "code" -> 200))
              case r if r.n < 1 => ok(Json.obj("message" -> "Fail insert new visitor view !!", "code" -> ERROR_BADREQUEST))
            }
        }
      case visitors if visitors.isEmpty =>
        visitorViewService.insertVisitorView(
          VisitorView(
            Some(BSONObjectID.generate()),
            request.remoteAddress,
            Some(new DateTime()),
            1
          )
        ).flatMap {
            case r if r.n > 0 => ok(Json.obj("message" -> "Successful insert new visitor view !!", "code" -> 200))
            case r if r.n < 1 => ok(Json.obj("message" -> "Fail insert new visitor view !!", "code" -> ERROR_BADREQUEST))
          }
    }
  }

  implicit val visitorViewReads: Reads[(String, String)] =
    (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] tupled
  def listVisitorViews() = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String)] {
      case (fromDate, toDate) =>
        visitorViewService.getVisitorViews("", fromDate, toDate).flatMap(
          visitors => ok(Json.obj("message" -> "Successful get visitor views !!", "code" -> 200, "visitors" -> visitors))
        ).fallbackTo(errorInternal)
    }
  }

  def listVisitorViewByYear(year: Int, month: Int, day: Int) = ApiAction { implicit request =>
    visitorViewService.getFilterVisitors(year, month, day).flatMap(
      visitors => ok(Json.obj("message" -> "Successful get visitor views !!", "code" -> 200, "visitors" -> visitors))
    ).fallbackTo(errorInternal)
  }

}
