package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.User.IUserService
import services.store.IStoreService
import reactivemongo.play.json.BSONFormats._
import play.api.libs.functional.syntax._
import api.ApiError._
import utils.ImageBase64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StoreController @Inject() (val storeService: IStoreService, val userService: IUserService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  implicit val updateStoreReads: Reads[(String, String, String)] =
    (__ \ "userId").read[String] and
      (__ \ "storeName").read[String] and
      (__ \ "storeInformation").read[String]tupled

  def updateStore() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String)] {
      case (userId, storeName, storeInformation) =>
        storeService.updateStoreByUserId(userId, storeName, storeInformation).flatMap {
          case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update store", "code" -> ERROR_BADREQUEST))
          case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful update store", "code" -> 200))
        }

    }
  }

  def getStoreByUserId(userId: String) = SecuredApiAction { implicit request =>
    storeService.getStoreByUserId(userId).flatMap {
      case None => ok(Json.obj("message" -> "store not found !!", "code" -> ERROR_USER_ID))
      case Some(store) => ok(Json.obj("store" -> store, "code" -> 200))
    }
  }

  implicit val updateStoreMapReads: Reads[(String, Double, Double)] =
    (__ \ "userId").read[String] and
      (__ \ "latitude").read[Double] and
      (__ \ "longitude").read[Double]tupled

  def updateStoreMap() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Double, Double)] {
      case (userId, latitude, longitude) =>
        storeService.updateStoreMapByUserId(userId, latitude, longitude).flatMap {
          case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to update store", "code" -> ERROR_BADREQUEST))
          case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Your location successfully saved!", "code" -> 200))
        }
    }

  }

  def uploadStoreBanner(userId: String) = SecuredApiActionWithBodyFile { request =>
    request.body.file("file").map { file =>
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val storeBanner = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      //val contentType = file.contentType
      file.ref.moveTo(new File(ImageBase64.storeBannerPath + storeBanner))
      storeService.getStoreByUserId(userId).flatMap {
        case None => ok(Json.obj("message" -> "User not found !!", "code" -> ERROR_USER_NOTFOUND))
        case Some(store) =>
          if (store.storeBanner.get != "") {
            ImageBase64.removeFile(ImageBase64.storeBannerPath + store.storeBanner.get)
            storeService.uploadStoreBanner(userId, storeBanner).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Success save image !!", "code" -> 200, "banner" -> storeBanner))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          } else {
            storeService.uploadStoreBanner(userId, storeBanner).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Success save image !!", "code" -> 200, "banner" -> storeBanner))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          }
      }
    }.getOrElse {
      ok(Json.obj("message" -> "Fail upload image !!", "code" -> ERROR_BADREQUEST))
    }
  }

  /*chivon*/
  def listUserWithStoreInfo(username: String) = ApiAction { implicit request =>
    userService.getUserWithStoreInfoService(username).flatMap(
      user => ok(Json.obj(
        "message" -> "Products found",
        "code" -> 200,
        "products" -> user
      ))
    ).fallbackTo(errorInternal)
  }

}
