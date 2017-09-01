package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import services.mistake.IMistakeService
import models.mistake.Mistake
import utils.FormatDate
import org.joda.time.DateTime
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Acer on 4/11/2017.
 */
class AdminManageMistake @Inject() (mistakeService: IMistakeService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  def insertMistake = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Mistake] {
      case mistake => {
        mistakeService.insertMistake(
          Mistake(
            Some(BSONObjectID.generate()),
            mistake.description,
            Some(FormatDate.parseDate(FormatDate.printDate(mistake.mistakeDate.get))),
            Some(0)
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully added new mistake",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with add new mistake",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }
  def updateMistake(id: String) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Mistake] {
      mistake =>
        mistakeService.updateMistakeById(
          id,
          Mistake(
            Some(BSONObjectID.parse(id).get),
            mistake.description,
            Some(FormatDate.parseDate(FormatDate.printDate(mistake.mistakeDate.get))),
            mistake.deleted
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully updated mistake",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with update mistake",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
    }

  }
  def deleteMistake(id: String) = SecuredApiAction { implicit request =>
    mistakeService.deleteMistake(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully deleted mistake",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with delete mistake",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  def listMistakes(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    mistakeService.listMistakes(page, limit).flatMap {
      case mistakes if mistakes.nonEmpty =>
        ok(Json.obj(
          "message" -> "Mistakes found",
          "code" -> 200,
          "mistakes" -> mistakes
        ))
      case mistake if mistake.isEmpty =>
        ok(Json.obj(
          "message" -> "Mistakes not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }
}
