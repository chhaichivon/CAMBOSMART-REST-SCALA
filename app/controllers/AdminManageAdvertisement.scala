package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.advertisement.{ Advertise, Advertisement, Advertiser }
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.advertisement.{ IAdvertisementService, IAdvertiserService, ICategoryAdvertisementService }
import play.api.libs.functional.syntax._
import reactivemongo.play.json.BSONFormats._
import reactivemongo.bson.BSONObjectID
import utils.{ FormatDate, ImageBase64 }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class AdminManageAdvertisement @Inject() (
    val advertisementService: IAdvertisementService,
    val advertiserService: IAdvertiserService,
    val categoryAdvertisementService: ICategoryAdvertisementService,
    val messagesApi: MessagesApi,
    system: ActorSystem
) extends api.ApiController {

  def insertAdvertisement() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Advertisement] {
      advertisement =>
        advertisementService.validatePageAndLocation(advertisement.page).flatMap {
          case advertisements if advertisements.nonEmpty =>
            if (advertisements.exists(_.location == advertisement.location)) {
              ok(Json.obj("message" -> "This advertisement already exist !!", "code" -> ERROR_USER_EXIST))
            } else {
              advertisementService.insertAdvertisement(
                Advertisement(
                  advertisement._id,
                  advertisement.page,
                  advertisement.location,
                  advertisement.description,
                  advertisement.price,
                  Some(List())
                )
              ).flatMap {
                  case r if r.n > 0 => created(Json.obj("message" -> "Successful add new advertisement !!", "code" -> 200))
                  case r if r.n < 1 => ok(Json.obj("message" -> "Fail add new advertisement !!", "code" -> ERROR_BADREQUEST))
                }.fallbackTo(errorInternal)
            }
          case advertisements if advertisements.isEmpty =>
            advertisementService.insertAdvertisement(
              Advertisement(
                advertisement._id,
                advertisement.page,
                advertisement.location,
                advertisement.description,
                advertisement.price,
                Some(List())
              )
            ).flatMap {
                case r if r.n > 0 => created(Json.obj("message" -> "Successful add new advertisement !!", "code" -> 200))
                case r if r.n < 1 => ok(Json.obj("message" -> "Fail add new advertisement !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
        }
    }
  }

  def updateAdvertisement() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Advertisement] {
      advertisement =>
        advertisementService.updateAdvertisement(advertisement).flatMap {
          case r if r.n > 0 => created(Json.obj("message" -> "Successful update advertisement !!", "code" -> 200))
          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertisement !!", "code" -> ERROR_BADREQUEST))
        }.fallbackTo(errorInternal)
    }
  }

  def deleteAdvertisement(id: String) = SecuredApiAction { implicit request =>
    advertisementService.deleteAdvertisement(id).flatMap {
      case r if r.n > 0 => created(Json.obj("message" -> "Successful delete advertisement !!", "code" -> 200))
      case r if r.n < 1 => ok(Json.obj("message" -> "Fail delete advertisement !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  /** validate **/
  def validatePageAndLocation(page: String) = SecuredApiAction { implicit request =>
    advertisementService.validatePageAndLocation(page).flatMap {
      advertisements =>
        ok(
          Json.obj(
            "message" -> "Successful get advertisements !!",
            "code" -> 200,
            "advertisements" -> advertisements
          )
        )
    }.fallbackTo(errorInternal)
  }

  def detailAdvertisement(id: String) = SecuredApiAction { implicit request =>
    advertisementService.getAdvertisementById(id).flatMap {
      case Some(advertisement) =>
        ok(
          Json.obj(
            "message" -> "Successful get advertisement !!",
            "code" -> 200,
            "advertisement" -> advertisement
          )
        )
      case None =>
        ok(
          Json.obj(
            "message" -> "Fail get advertisement !!",
            "code" -> ERROR_NOTFOUND
          )
        )
    }.fallbackTo(errorInternal)
  }

  /**
   * Function listAdvertisementByPageAndLocation list all active advertisement
   * @param page
   * @param location
   * @return
   */
  def listAdvertisementByPageAndLocation(page: String, location: String) = SecuredApiAction { implicit request =>
    advertisementService.getAdvertisementByPageAndLocation(page, location).flatMap {
      advertisements =>
        ok(
          Json.obj(
            "message" -> "Successful get advertisement !!",
            "code" -> 200,
            "advertisements" -> advertisements
          )
        )
    }.fallbackTo(errorInternal)
  }

  def listScheduleAdvertisement(page: String, location: String, start: Int, limit: Int) = SecuredApiAction { implicit request =>
    advertisementService.getAdvertisementAndAdvertiserByPageAndLocation(page, location, start, limit).flatMap {
      advertisements =>
        ok(
          Json.obj(
            "message" -> "Successful get advertisements !!",
            "code" -> 200,
            "advertisements" -> advertisements
          )
        )
    }.fallbackTo(errorInternal)
  }
  /**======================================================Advertiser management===================================================**/

  implicit val advertiserReads: Reads[(String, String, Advertiser, Advertise)] =
    (__ \ "page").read[String] and
      (__ \ "location").read[String] and
      (__ \ "advertiser").read[Advertiser] and
      (__ \ "advertise").read[Advertise] tupled
  def insertAdvertiser() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Advertiser, Advertise)] {
      case (page, location, advertiser, advertise) =>
        val id = BSONObjectID.generate()
        advertisementService.getAdvertisementByPageAndLocation(page, location).flatMap {
          case advertisements if advertisements.nonEmpty =>
            val ads = (advertisements.head \ "advertise").as[List[JsObject]]
            if (ads.length >= 20) {
              ok(Json.obj("message" -> "This advertisement are full, cannot add more !!", "code" -> ERROR_USER_EXIST))
            } else {
              if (ads.length == 10 && advertise.expireDate.getMillis > (ads.head \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 11 && advertise.expireDate.getMillis > (ads(1) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 12 && advertise.expireDate.getMillis > (ads(2) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 13 && advertise.expireDate.getMillis > (ads(3) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 14 && advertise.expireDate.getMillis > (ads(4) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 15 && advertise.expireDate.getMillis > (ads(5) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 16 && advertise.expireDate.getMillis > (ads(6) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 17 && advertise.expireDate.getMillis > (ads(7) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 18 && advertise.expireDate.getMillis > (ads(8) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length == 19 && advertise.expireDate.getMillis > (ads(9) \ "expireDate").as[Long]) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else if (ads.length < 10) {
                advertiserService.insertAdvertiser(
                  Advertiser(
                    Some(id),
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
                      advertisementService.updateAdvertiseInAdvertisement(
                        page,
                        location,
                        Advertise(
                          Some(id),
                          advertise.duration,
                          advertise.startDate,
                          advertise.expireDate,
                          advertise.price,
                          advertise.discount
                        )
                      ).flatMap {
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                        }
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              } else {
                ok(Json.obj("message" -> "This advertisement are busy, cannot add more !!", "code" -> ERROR_USER_EXIST))
              }
            }
          case advertisements if advertisements.isEmpty =>
            advertiserService.insertAdvertiser(
              Advertiser(
                Some(id),
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
                  advertisementService.updateAdvertiseInAdvertisement(
                    page,
                    location,
                    Advertise(
                      Some(id),
                      advertise.duration,
                      advertise.startDate,
                      advertise.expireDate,
                      advertise.price,
                      advertise.discount
                    )
                  ).flatMap {
                      case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertise in advertisement !!", "code" -> 200, "id" -> id.stringify))
                      case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertise in advertisement !!", "code" -> ERROR_BADREQUEST))
                    }
                case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new advertiser !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
          /*Some(id),
          advertise.duration,
          FormatDate.parseDate(FormatDate.printDate(advertise.startDate)),
          FormatDate.parseDate(FormatDate.printDate(advertise.expireDate)),
          advertise.price,
          advertise.discount*/
        }
    }
  }

  def updateAdvertiser() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Advertiser] { advertiser =>
      advertiserService.updateAdvertiser(advertiser).flatMap {
        case r if r.n > 0 => ok(Json.obj("message" -> "Successful update advertiser !!", "code" -> 200))
        case r if r.n < 1 => ok(Json.obj("message" -> "Fail update advertiser !!", "code" -> ERROR_BADREQUEST))
      }.fallbackTo(errorInternal)
    }
  }

  def advertiserUploadImage(id: String) = SecuredApiActionWithBodyFile { request =>
    request.body.file("file").map { file =>
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val filename = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      file.ref.moveTo(new File(ImageBase64.advertisementPath + filename))
      advertiserService.getAdvertiserById(id).flatMap {
        case None => ok(Json.obj("message" -> "Advertiser not found !!", "code" -> ERROR_NOTFOUND))
        case Some(advertiser) =>
          if (advertiser.image != "") {
            advertiserService.uploadAdvertiserImage(id, filename).flatMap {
              case writeRes if writeRes.n > 0 =>
                ImageBase64.removeFile(ImageBase64.advertisementPath + advertiser.image)
                ok(Json.obj("message" -> "Success save image !!", "code" -> 200))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          } else {
            advertiserService.uploadAdvertiserImage(id, filename).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Success save image !!", "code" -> 200))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image !!", "code" -> ERROR_BADREQUEST))
            }.fallbackTo(errorInternal)
          }
      }
    }.getOrElse {
      ok(Json.obj("message" -> "Fail upload image !!", "code" -> ERROR_BADREQUEST))
    }
  }

  def blockAdvertiser(id: String, check: Int) = SecuredApiAction { implicit request =>
    advertiserService.blockAdvertiser(id, -1).flatMap {
      case writeResult if writeResult.n > 0 =>
        if (check > 0) {
          advertisementService.updateAdvertise(id).flatMap(
            result => ok(Json.obj("message" -> "Successful update advertiser !!", "code" -> 200))
          ).fallbackTo(errorInternal)
        } else {
          categoryAdvertisementService.updateAdvertise(id).flatMap(
            result => ok(Json.obj("message" -> "Successful update advertiser !!", "code" -> 200))
          ).fallbackTo(errorInternal)
        }
      case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail update advertiser !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  implicit val reAdvertiseReads: Reads[(String, String, String, Advertise)] =
    (__ \ "id").read[String] and
      (__ \ "page").read[String] and
      (__ \ "location").read[String] and
      (__ \ "advertise").read[Advertise] tupled
  def renewAdvertisement() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, Advertise)] {
      case (id, page, location, advertise) =>
        println("category id : " + id)
        if (id != "") {
          categoryAdvertisementService.getCategoryAdvertisementSchedule(id).flatMap {
            case advertisements if advertisements.nonEmpty =>
              val expireDate = (advertisements.head \ "expireDate").as[Long]
              if (advertisements.length >= 2) {
                ok(Json.obj("message" -> "This advertisement are full, cannot add more !!", "code" -> ERROR_USER_EXIST))
              } else {
                if (advertise.expireDate.getMillis > expireDate) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      categoryAdvertisementService.updateCategoryAdvertisementInPackage(id, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else {
                  ok(Json.obj("message" -> "This advertisement are busy, cannot add more !!", "code" -> ERROR_USER_EXIST))
                }
              }
            case advertisements if advertisements.isEmpty =>
              advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                case writeResult if writeResult.n > 0 =>
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case r if r.n > 0 =>
                      categoryAdvertisementService.updateCategoryAdvertisementInPackage(id, advertise).flatMap {
                        case re if re.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case re if re.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
          }
        } else {
          advertisementService.getAdvertisementByPageAndLocation(page, location).flatMap {
            case advertisements if advertisements.nonEmpty =>
              val ads = (advertisements.head \ "advertise").as[List[JsObject]]
              if (ads.length >= 20) {
                ok(Json.obj("message" -> "This advertisement are full, cannot add more !!", "code" -> ERROR_USER_EXIST))
              } else {
                if (ads.length == 10 && advertise.expireDate.getMillis > (ads.head \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 11 && advertise.expireDate.getMillis > (ads(1) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 12 && advertise.expireDate.getMillis > (ads(2) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 13 && advertise.expireDate.getMillis > (ads(3) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 14 && advertise.expireDate.getMillis > (ads(4) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 15 && advertise.expireDate.getMillis > (ads(5) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 16 && advertise.expireDate.getMillis > (ads(6) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 17 && advertise.expireDate.getMillis > (ads(7) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 18 && advertise.expireDate.getMillis > (ads(8) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length == 19 && advertise.expireDate.getMillis > (ads(9) \ "expireDate").as[Long]) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else if (ads.length < 10) {
                  advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                    case writeResult if writeResult.n > 0 =>
                      advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                        case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                        case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                      }.fallbackTo(errorInternal)
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                } else {
                  ok(Json.obj("message" -> "This advertisement are busy, cannot add more !!", "code" -> ERROR_USER_EXIST))
                }
              }
            case advertisements if advertisements.isEmpty =>
              advertiserService.blockAdvertiser(advertise.id.get.stringify, 1).flatMap {
                case writeResult if writeResult.n > 0 =>
                  advertisementService.updateAdvertiseInAdvertisement(page, location, advertise).flatMap {
                    case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew advertise !!", "code" -> 200))
                    case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
                case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail renew advertise !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
          }
        }
    }
  }

  implicit val getAdvertiserReads: Reads[(Option[BSONObjectID], String, Long, Long)] =
    (__ \ "id").readNullable[BSONObjectID] and
      (__ \ "location").read[String] and
      (__ \ "startDate").read[Long] and
      (__ \ "expireDate").read[Long] tupled
  def listAdvertiser() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(Option[BSONObjectID], String, Long, Long)] {
      case (id, location, startDate, expireDate) =>
        advertisementService.getAdvertiserById(id.get, location, startDate, expireDate).flatMap {
          case advertisers if advertisers.nonEmpty =>
            ok(Json.obj("message" -> "Successful get advertiser !!", "code" -> 200, "advertiser" -> advertisers.head))
          case advertisers if advertisers.isEmpty =>
            ok(Json.obj("message" -> "Fail get advertiser !!", "code" -> ERROR_NOTFOUND))
        }.fallbackTo(errorInternal)
    }
  }

  implicit val filterAdvertiserReads: Reads[(String, String, Int, String, String)] =
    (__ \ "name").read[String] and
      (__ \ "location").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "startDate").read[String] and
      (__ \ "endDate").read[String] tupled
  def listAdvertisers(start: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, String, String)] {
      case (name, location, status, startDate, endDate) =>
        advertisementService.getAdvertisers(name, location, status, startDate, endDate, start, limit).flatMap {
          advertisers =>
            ok(
              Json.obj(
                "message" -> "Successful get advertisers !!",
                "code" -> 200,
                "advertisers" -> advertisers
              )
            )
        }.fallbackTo(errorInternal)
    }
  }

  def displayAdvertisements() = ApiAction { implicit request =>
    advertisementService.getDisplayAdvertisements.flatMap(
      advertisements => ok(
        Json.obj(
          "message" -> "Successful get display advertisements !!",
          "code" -> 200,
          "advertisements" -> advertisements
        )
      )
    ).fallbackTo(errorInternal)
  }

}
