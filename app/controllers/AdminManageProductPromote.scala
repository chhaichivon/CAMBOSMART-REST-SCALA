package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.promoted.PromoteProduct
import org.joda.time.DateTime
import reactivemongo.bson.BSONObjectID
import services.promoted.IPromoteProductService
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import utils.FormatDate
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by Ky Sona on 3/9/2017.
 */
class AdminManageProductPromote @Inject() (promoteProductService: IPromoteProductService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /* ADMIN FILTER USERS REQUESTS */
  implicit val filterUsersReads: Reads[(String, String, String, String)] =
    (__ \ "city").read[String] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] and
      (__ \ "name").read[String] tupled

  def listUsers(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String)] {
      case (city, fromDate, toDate, name) => {
        promoteProductService.listUserPromoteProduct(city, fromDate, toDate, name, page, limit).flatMap {
          case users if users.nonEmpty =>
            ok(Json.obj(
              "message" -> "Users found",
              "code" -> 200,
              "users" -> users
            ))
          case users if users.isEmpty =>
            ok(Json.obj(
              "message" -> "Users not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* LIST PROMOTE PRODUCTS REQUEST BY USER ID */
  def listPromoteProductByUserId(promoteId: String, userId: String) = SecuredApiAction { implicit request =>
    promoteProductService.listPromoteProductByUserId(promoteId, userId).flatMap {
      case products if products.nonEmpty =>
        ok(Json.obj(
          "message" -> "Products found",
          "code" -> 200,
          "products" -> products
        ))
      case products if products.isEmpty =>
        ok(Json.obj(
          "message" -> "Products not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  implicit val approveUserRequestReads: Reads[(String, List[String], String)] =
    (__ \ "id").read[String] and
      (__ \ "ids").read[List[String]] and
      (__ \ "promoteType").read[String] tupled
  /* ADMIN APPROVE USER REQUESTED */
  def approveUserRequestById = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, List[String], String)] {
      case (id, ids, promoteType) => {
        promoteProductService.approveProductById(ids, promoteType).flatMap {
          case writeRes if writeRes.n > 0 => {
            promoteProductService.approvedPromotedRequestById(id).flatMap {
              case writeRes if writeRes.n > 0 =>
                ok(Json.obj(
                  "message" -> "Successfully approved user request",
                  "code" -> 200
                ))
              case writeRes if writeRes.n < 1 =>
                ok(Json.obj(
                  "message" -> "Fail with approve user request",
                  "code" -> 500
                ))
            }
          }
          case writeRes if writeRes.n < 1 =>
            ok(Json.obj(
              "message" -> "Fail with change product type",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  implicit val changeProductTypeReads: Reads[(List[String], String)] =
    (__ \ "ids").read[List[String]] and
      (__ \ "promoteType").read[String] tupled
  /* ADMIN CHANGE TYPE PRODUCTS */
  def changeTypeProductsById = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(List[String], String)] {
      case (ids, promoteType) => {
        promoteProductService.approveProductById(ids, promoteType).flatMap {
          case writeRes if writeRes.n > 0 =>
            ok(Json.obj(
              "message" -> "Successfully change product type",
              "code" -> 200
            ))
          case writeRes if writeRes.n < 1 =>
            ok(Json.obj(
              "message" -> "Fail with change product type",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* ADMIN APPROVED USER REQUEST */
  def approvedUserRequestById(id: String) = SecuredApiAction { implicit request =>
    promoteProductService.approvedPromotedRequestById(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully approved user request",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with approve user request",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN DELETE USER REQUEST */
  def deleteUserRequest(id: String) = SecuredApiAction { implicit request =>
    promoteProductService.deleteUserRequestById(id).flatMap {
      case writeRes if writeRes.n > 0 =>
        ok(Json.obj(
          "message" -> "Successfully deleted user request",
          "code" -> 200
        ))
      case writeRes if writeRes.n < 1 =>
        ok(Json.obj(
          "message" -> "Fail with delete user request",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* DELETE A PRODUCT REQUEST */
  implicit val deleteProductReads: Reads[(String, String)] =
    (__ \ "productId").read[String] and
      (__ \ "id").read[String] tupled

  def deleteProductRequestById = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String)] {
      case (productId, id) => {
        promoteProductService.deleteProductRequestById(productId).flatMap {
          case writeRes if writeRes.n > 0 =>
            ok(Json.obj(
              "message" -> "Successfully update delet product request",
              "code" -> 200
            ))
          case writeRes if writeRes.n < 1 =>
            ok(Json.obj(
              "message" -> "Fail with delete promote product by ID",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }

  /* MEMBER ADD PROMOTED PRODUCTS */
  def insertPromotedProducts = SecuredApiActionWithBody { implicit request =>
    readFromRequest[PromoteProduct] {
      case product => {
        promoteProductService.insertPromoteProducts(
          PromoteProduct(
            Some(BSONObjectID.generate()),
            product.userId,
            product.packageId,
            product.typePromote,
            product.duration,
            product.price,
            product.productId,
            Some(FormatDate.parseDate(FormatDate.printDate(product.startDate.get))),
            Some(FormatDate.parseDate(FormatDate.printDate(product.endDate.get))),
            product.status
          )
        ).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj(
                "message" -> "Successfully add new promoted products",
                "code" -> 200
              ))
            case writeRes if writeRes.n < 1 =>
              ok(Json.obj(
                "message" -> "Fail with add new promoted products",
                "code" -> 500
              ))
          }.fallbackTo(errorInternal)
      }
    }
  }

  /* ADMIN LIST PROMOTED PRODUCTS EXPIRED */
  def listPromotedProductsExpired(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    promoteProductService.listPromotedProductsExpired(page, limit).flatMap {
      case promoted if promoted.nonEmpty => {
        ok(Json.obj(
          "message" -> "Successfully get expired promoted products",
          "code" -> 200,
          "promoted" -> promoted
        ))
      }
      case promoted if promoted.isEmpty =>
        ok(Json.obj(
          "message" -> "Fail with get expired promoted products",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN UPDATE PROMOTED PRODUCTS EXPIRED */
  def updatePromotedProductExpired = SecuredApiAction { implicit request =>
    promoteProductService.updatePromotedProductExpired().flatMap {
      case promoted if promoted.nonEmpty => {
        ok(Json.obj(
          "message" -> "Successfully updated expired promoted",
          "code" -> 200
        ))
      }
      case promoted if promoted.isEmpty =>
        ok(Json.obj(
          "message" -> "Fail with updated expired promoted",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

}
