package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.notification.Notifications
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import services.notification.INotificationService

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationController @Inject() (val notificationService: INotificationService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {
  def insertNotification() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Notifications] { notification =>
      notificationService.insertNotification(
        Notifications(
          notification._id,
          notification.userId,
          notification.notificationType,
          notification.description,
          notification.isView,
          notification.isDirty,
          Some(new DateTime())
        )
      ).flatMap {
          result =>
            ok(Json.obj(
              "message" -> "Successful insert notification !",
              "code" -> 200
            ))
        }
    }
  }

  def getAllNotificationsByUserId(userId: String, limit: Int) = SecuredApiAction { implicit request =>
    notificationService.getAllNotificationsByUserId(BSONObjectID.parse(userId).get, limit).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful get notification by user id !",
        "code" -> 200,
        "data" -> result
      ))
    ).fallbackTo(errorInternal)
  }

  def updateAllViewNotification() = SecuredApiActionWithBody { implicit request =>
    val userId = (request.body \ "userId").as[String]
    notificationService.updateAllViewNotification(BSONObjectID.parse(userId).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful update all notification not yet view !",
        "code" -> 200
      ))
    ).fallbackTo(errorInternal)
  }

  def updateDirtyNotification() = SecuredApiActionWithBody { implicit request =>
    val id = (request.body \ "id").as[String]
    notificationService.updateDirtyNotification(BSONObjectID.parse(id).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful update dirty notification !",
        "code" -> 200
      ))
    ).fallbackTo(errorInternal)
  }

  def countNotificationUnView(userId: String) = SecuredApiAction { implicit request =>
    notificationService.countNotificationUnView(BSONObjectID.parse(userId).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful count notification !",
        "code" -> 200,
        "data" -> result
      ))
    ).fallbackTo(errorInternal)
  }

  def getNotificationById(id: String) = SecuredApiAction { implicit request =>
    notificationService.getNotificationById(BSONObjectID.parse(id).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful get notification by id !",
        "code" -> 200,
        "data" -> result
      ))
    ).fallbackTo(errorInternal)
  }
}