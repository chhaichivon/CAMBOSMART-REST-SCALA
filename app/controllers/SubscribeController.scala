package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.subscribe.Subscribe
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import services.subscribe.ISubscribeService

import scala.concurrent.ExecutionContext.Implicits.global

class SubscribeController @Inject() (val subscribeService: ISubscribeService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {
  def insertSubscribe() = SecuredApiActionWithBody { implicit request =>
    val subscribe = request.body.as[Subscribe]
    subscribeService.insertSubscribe(subscribe).flatMap {
      result =>
        ok(Json.obj(
          "message" -> "Successful insert subscribe !!",
          "code" -> 200
        ))
    }
  }

  def getSubscribeByStoreIdAndUserId(storeId: String, userId: String) = SecuredApiAction { implicit request =>
    subscribeService.getSubscribeByStoreIdAndUserId(BSONObjectID.parse(storeId).get, BSONObjectID.parse(userId).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful get subscribe !!",
        "code" -> 200,
        "data" -> result
      ))
    ).fallbackTo(errorInternal)
  }

  def deleteSubscribe(id: String) = SecuredApiAction { implicit request =>
    subscribeService.deleteSubscribe(BSONObjectID.parse(id).get).flatMap(
      result => ok(Json.obj(
        "message" -> "Successful delete subscribe !!",
        "code" -> 200
      ))
    ).fallbackTo(errorInternal)
  }

  def getUserHasSubscribeByStoreId(storeId: String) = SecuredApiAction { implicit request =>
    subscribeService.getUserHasSubscribeByStoreId(BSONObjectID.parse(storeId).get).flatMap(
      result => ok(result)
    ).fallbackTo(errorInternal)
  }
}
