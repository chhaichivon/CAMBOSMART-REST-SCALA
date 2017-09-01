package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.advertisement.{ Advertise, Advertiser, CategoryAdvertisement }
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import play.api.libs.functional.syntax._
import reactivemongo.play.json.BSONFormats._
import services.advertisement.{ IAdvertiserService, ICategoryAdvertisementService }
import utils.FormatDate

import scala.concurrent.ExecutionContext.Implicits.global

class AdminManageCategoryAdvertisement @Inject() (val categoryAdvertisementService: ICategoryAdvertisementService, val advertiserService: IAdvertiserService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  def insertCategoryAdvertisement() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[CategoryAdvertisement] { categoryAdvertisement =>
      categoryAdvertisementService.getCategoryAdvertisementByCategoryId(categoryAdvertisement.categoryId.get).flatMap {
        case Some(data) =>
          ok(Json.obj("message" -> "This category advertisement already exist !!", "code" -> ERROR_USER_EXIST))
        case None =>
          categoryAdvertisementService.insertCategoryAdvertisement(
            CategoryAdvertisement(
              categoryAdvertisement._id,
              categoryAdvertisement.categoryId,
              categoryAdvertisement.name,
              categoryAdvertisement.description,
              categoryAdvertisement.price,
              Some(List())
            )
          ).flatMap {
              case r if r.n > 0 => created(Json.obj("message" -> "Successful add new category advertisement !!", "code" -> 200))
              case r if r.n < 1 => ok(Json.obj("message" -> "Fail add new category advertisement !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
      }
    }
  }

  def updateCategoryAdvertisement() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[CategoryAdvertisement] { categoryAdvertisement =>
      categoryAdvertisementService.updateCategoryAdvertisement(categoryAdvertisement).flatMap {
        case r if r.n > 0 => created(Json.obj("message" -> "Successful update category advertisement !!", "code" -> 200))
        case r if r.n < 1 => ok(Json.obj("message" -> "Fail update category advertisement !!", "code" -> ERROR_BADREQUEST))
      }.fallbackTo(errorInternal)
    }
  }

  def deleteCategoryAdvertisement(id: String) = SecuredApiAction { implicit request =>
    categoryAdvertisementService.deleteCategoryAdvertisement(id).flatMap {
      case r if r.n > 0 => created(Json.obj("message" -> "Successful delete category advertisement !!", "code" -> 200))
      case r if r.n < 1 => ok(Json.obj("message" -> "Fail delete category advertisement !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  def viewCategoryAdvertisement(id: String) = SecuredApiAction { implicit request =>
    categoryAdvertisementService.getCategoryAdvertisementByCategoryId(BSONObjectID.parse(id).get).flatMap(
      advertisement => ok(Json.obj("message" -> "Successful get category advertisement !!", "code" -> 200, "advertisement" -> advertisement))
    ).fallbackTo(errorInternal)
  }

  def listCategoryAdvertisement() = SecuredApiAction { implicit request =>
    categoryAdvertisementService.getCategoryAdvertisements.flatMap(
      advertisements => ok(Json.obj("message" -> "Successful get category advertisements !!", "code" -> 200, "advertisements" -> advertisements))
    ).fallbackTo(errorInternal)
  }

  def listScheduleCategoryAdvertisement(id: String) = SecuredApiAction { implicit request =>
    categoryAdvertisementService.getCategoryAdvertisementSchedule(id).flatMap { advertisements =>
      ok(
        Json.obj(
          "message" -> "Successful get category advertisement schedule !!",
          "code" -> 200,
          "advertisements" -> advertisements
        )
      )
    }.fallbackTo(errorInternal)
  }

  /**======================================================Advertiser management===================================================**/

  implicit val advertiserReads: Reads[(String, Advertiser, Advertise)] =
    (__ \ "id").read[String] and
      (__ \ "advertiser").read[Advertiser] and
      (__ \ "advertise").read[Advertise] tupled
  def insertAdvertiser() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Advertiser, Advertise)] {
      case (id, advertiser, advertise) =>
        val oid = BSONObjectID.generate()
        categoryAdvertisementService.getCategoryAdvertisementSchedule(id).flatMap {
          case advertisements if advertisements.nonEmpty =>
            if (advertisements.length == 1) {
              val expireDate = (advertisements.head \ "expireDate").as[Long]
              if (advertise.expireDate.getMillis > expireDate) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(oid),
                    advertiser.name,
                    advertiser.description,
                    advertiser.phones,
                    advertiser.email,
                    advertiser.city,
                    advertiser.address,
                    advertiser.image,
                    advertiser.url,
                    Some(new DateTime()),
                    Some(1)
                  )
                ).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      categoryAdvertisementService.updateCategoryAdvertisementInPackage(
                        id,
                        Advertise(
                          Some(oid),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(
                            Json.obj(
                              "message" -> "Successful update advertise in advertisement !!",
                              "code" -> 200,
                              "id" -> oid.stringify
                            )
                          )
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else {
                ok(Json.obj("message" -> "This advertisement are full, cannot add more !!", "code" -> ERROR_USER_EXIST))
              }
            } else {
              ok(Json.obj("message" -> "This advertisement are full, cannot add more !!", "code" -> ERROR_USER_EXIST))
            }
          case advertisements if advertisements.isEmpty =>
            advertiserService.insertAdvertiser(
              Advertiser(
                Some(oid),
                advertiser.name,
                advertiser.description,
                advertiser.phones,
                advertiser.email,
                advertiser.city,
                advertiser.address,
                advertiser.image,
                advertiser.url,
                Some(new DateTime()),
                Some(1)
              )
            ).flatMap {
                case writeResult if writeResult.n > 0 =>
                  categoryAdvertisementService.updateCategoryAdvertisementInPackage(
                    id,
                    Advertise(
                      Some(oid),
                      advertise.duration,
                      advertise.startDate,
                      advertise.expireDate,
                      advertise.price,
                      advertise.discount
                    )
                  ).flatMap {
                      case r if r.n > 0 => ok(
                        Json.obj(
                          "message" -> "Successful update advertise in advertisement !!",
                          "code" -> 200,
                          "id" -> oid.stringify
                        )
                      )
                      case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                    }
                case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
        }.fallbackTo(errorInternal)
    }
  }

  implicit val filterAdvertiserReads: Reads[(String, String, Int, String, String)] =
    (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] tupled
  def listCategoryAdvertisers(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, String, String)] {
      case (name, location, status, startDate, endDate) =>
        categoryAdvertisementService.getCategoryAdvertisers(name, location, status, startDate, endDate, page, limit).flatMap { advertisers =>
          ok(
            Json.obj(
              "message" -> "Successful get category advertisers !!",
              "code" -> 200,
              "advertisers" -> advertisers
            )
          )
        }.fallbackTo(errorInternal)
    }
  }

  def getCategoryAdvertiser(id: String, startDate: Long, expireDate: Long) = SecuredApiAction { implicit request =>
    categoryAdvertisementService.getCategoryAdvertiser(id, startDate, expireDate).flatMap {
      case advertisers if advertisers.nonEmpty =>
        ok(Json.obj("message" -> "Successful get category advertisement schedule !!", "code" -> 200, "advertiser" -> advertisers.head))
      case advertisers if advertisers.isEmpty =>
        ok(Json.obj("message" -> "Successful get category advertisement schedule !!", "code" -> ERROR_NOTFOUND))
    }.fallbackTo(errorInternal)
  }

  def displayCategoryAdvertisements() = ApiAction { implicit request =>
    categoryAdvertisementService.getDisplayCategoryAdvertisements.flatMap {
      advertisements =>
        ok(Json.obj(
          "message" -> "Successful get display category advertisement !!",
          "code" -> 200,
          "advertisements" -> advertisements
        ))
    }.fallbackTo(errorInternal)
  }
}
