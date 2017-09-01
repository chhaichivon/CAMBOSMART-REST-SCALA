package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.advertiser.{ Advertiser, Advertising }
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsValue.jsValueToJsLookup
import reactivemongo.bson.BSONObjectID
import services.advertiser.AdvertiserService
import utils.FormatDate

import reactivemongo.play.json.BSONFormats._
import scala.concurrent.ExecutionContext.Implicits.global

class AdminManageAdvertiser @Inject() (advertiserService: AdvertiserService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  implicit val addAdvertiserReads: Reads[(Advertiser, Advertising)] =
    (__ \ "advertiser").read[Advertiser] and
      (__ \ "advertising").read[Advertising] tupled

  def addNewAdvertiser() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(Advertiser, Advertising)] {
      case (advertiser, advertising) =>
        val id = BSONObjectID.generate()
        advertiserService.insertAdvertiser(
          Advertiser(
            Some(id),
            advertiser.name,
            advertiser.description,
            advertiser.phones,
            advertiser.email,
            advertiser.city,
            advertiser.address,
            Some("advertiser"),
            Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
            Some(FormatDate.parseDate("1970-1-1")),
            Some(1)
          )
        ).flatMap {
            case r if r.n > 0 =>
              advertiserService.getAdvertisingByPageLocation(advertising.page, advertising.location, advertising.price, FormatDate.printDate(advertising.dateStart.get), FormatDate.printDate(advertising.dateEnd.get)).flatMap {
                case None =>
                  advertiserService.insertAdvertising(
                    Advertising(
                      Some(BSONObjectID.generate()),
                      Some(List(id)),
                      advertising.ads_name,
                      advertising.website,
                      advertising.description,
                      advertising.page,
                      advertising.location,
                      advertising.price,
                      Some(FormatDate.parseDate(FormatDate.printDate(advertising.dateStart.get))),
                      Some(FormatDate.parseDate(FormatDate.printDate(advertising.dateEnd.get))),
                      advertising.bannerImg,
                      Some(0),
                      advertising.userId
                    )
                  ).flatMap {
                      case re if re.n > 0 =>
                        println("Successful add !!"); created(Json.obj("message" -> "Successful to add new advertiser && advertising", "code" -> 201))
                      case re if re.n < 1 => println("err"); ok(Json.obj("message" -> "Fail to add new advertiser && advertising", "code" -> ERROR_BADREQUEST))
                    }
                case Some(ads) =>
                  var advertiserId = (ads \ "advertiserId").as[List[BSONObjectID]]
                  advertiserId = advertiserId :+ id
                  advertiserService.updateAdvertiserId(
                    (ads \ "_id").as[BSONObjectID].stringify, advertiserId
                  ).flatMap {
                    case re if re.n > 0 =>
                      println("Successful update !!"); created(Json.obj("message" -> "Successful to add new advertiser && advertising", "code" -> 201))
                    case re if re.n < 1 => println("err"); ok(Json.obj("message" -> "Fail to add new advertiser && advertising", "code" -> ERROR_BADREQUEST))
                  }
              }
            case r if r.n < 1 => ok(Json.obj("message" -> "Fail to add new advertiser", "code" -> ERROR_BADREQUEST))
          }.fallbackTo(errorInternal)
    }
  }

  def updateAdvertiser(id: String) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Advertiser] {
      advertiser =>
        advertiserService.updateAdvertiser(id, advertiser).flatMap {
          case r if r.n > 0 => created(Json.obj("message" -> "Successful update advertiser !!", "code" -> 201))
          case r if r.n < 1 => ok(Json.obj("message" -> "Successful update advertiser !!", "code" -> ERROR_BADREQUEST))
        }.fallbackTo(errorInternal)
    }
  }

  def updateAdvertising(id: String) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Advertising] {
      advertising =>
        advertiserService.updateAdvertising(
          id,
          Advertising(
            Some(BSONObjectID.parse(id).get),
            advertising.advertiserId,
            advertising.ads_name,
            advertising.website,
            advertising.description,
            advertising.page,
            advertising.location,
            advertising.price,
            Some(FormatDate.parseDate(FormatDate.printDate(advertising.dateStart.get))),
            Some(FormatDate.parseDate(FormatDate.printDate(advertising.dateEnd.get))),
            advertising.bannerImg,
            Some(0),
            advertising.userId
          )
        ).flatMap {
            case r if r.n > 0 => created(Json.obj("message" -> "Successful update advertiser !!", "code" -> 201))
            case r if r.n < 1 => ok(Json.obj("message" -> "Successful update advertiser !!", "code" -> ERROR_BADREQUEST))
          }.fallbackTo(errorInternal)
    }
  }

  def deleteAdvertiser(id: String, status: Int) = SecuredApiAction { implicit request =>
    advertiserService.deleteAdvertiser(id, status).flatMap {
      case r if r.n > 0 => ok(Json.obj("message" -> "Successful to delete advertiser !!", "code" -> 200))
      case r if r.n < 1 => ok(Json.obj("message" -> "Fail to delete advertiser !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  implicit val filterAdvertiserReads: Reads[(String, String, Int, String, String)] =
    (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String]tupled

  def listFilterAdvertisers(start: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, String, String)] {
      case (name, city, status, fromDate, toDate) =>
        advertiserService.getFilterAdvertisers(name, city, status, fromDate, toDate, start, limit).flatMap(advertisers => ok(advertisers)).fallbackTo(errorInternal)
    }
  }

  def viewAdvertiser(id: String) = SecuredApiAction { implicit request =>
    advertiserService.getAdvertiserById(id).flatMap {
      case None => ok(Json.obj("message" -> "Advertiser not found !!", "code" -> ERROR_NOTFOUND))
      case Some(advertiser) => ok(Json.obj("message" -> "Advertiser found !!", "code" -> 200, "advertiser" -> advertiser))
    }
  }
}
