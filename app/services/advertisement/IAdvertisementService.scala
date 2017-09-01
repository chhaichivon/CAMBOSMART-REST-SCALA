package services.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.{ Advertise, Advertisement }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.advertisement.IAdvertisementRepo

import scala.concurrent.Future

@ImplementedBy(classOf[AdvertisementService])
trait IAdvertisementService {
  def insertAdvertisement(advertisement: Advertisement): Future[WriteResult]
  def updateAdvertisement(advertisement: Advertisement): Future[WriteResult]
  def updateAdvertiseInAdvertisement(page: String, location: String, advertise: Advertise): Future[WriteResult]
  def updateAdvertise(advertiserId: String): Future[WriteResult]
  def deleteAdvertisement(advertisementId: String): Future[WriteResult]
  def getAdvertisementById(advertisementId: String): Future[Option[Advertisement]]
  def validatePageAndLocation(page: String): Future[List[Advertisement]]
  def getAdvertisementByPageAndLocation(page: String, location: String): Future[List[JsObject]]
  def getAdvertisementAndAdvertiserByPageAndLocation(page: String, location: String, start: Int, limit: Int): Future[List[JsObject]]
  def getAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, start: Int, limit: Int): Future[List[JsObject]]
  def getAdvertiserById(advertiserId: BSONObjectID, location: String, startDate: Long, expireDate: Long): Future[List[JsObject]]
  def getDisplayAdvertisements: Future[List[JsObject]]
}

class AdvertisementService @Inject() (val advertisementRepo: IAdvertisementRepo) extends IAdvertisementService {

  override def insertAdvertisement(advertisement: Advertisement): Future[WriteResult] = advertisementRepo.insertAdvertisement(advertisement)

  override def updateAdvertisement(advertisement: Advertisement): Future[WriteResult] = advertisementRepo.updateAdvertisement(advertisement)

  override def updateAdvertiseInAdvertisement(page: String, location: String, advertise: Advertise): Future[WriteResult] = advertisementRepo.updateAdvertiseInAdvertisement(page, location, advertise)

  override def updateAdvertise(advertiserId: String): Future[WriteResult] = advertisementRepo.updateAdvertise(advertiserId)

  override def deleteAdvertisement(advertisementId: String): Future[WriteResult] = advertisementRepo.deleteAdvertisement(advertisementId)

  override def getAdvertisementById(advertisementId: String): Future[Option[Advertisement]] = advertisementRepo.getAdvertisementById(advertisementId)

  override def validatePageAndLocation(page: String): Future[List[Advertisement]] = advertisementRepo.validatePageAndLocation(page)

  override def getAdvertisementByPageAndLocation(page: String, location: String): Future[List[JsObject]] = advertisementRepo.getAdvertisementByPageAndLocation(page, location)

  override def getAdvertisementAndAdvertiserByPageAndLocation(page: String, location: String, start: Int, limit: Int): Future[List[JsObject]] = advertisementRepo.getAdvertisementAndAdvertiserByPageAndLocation(page, location, start, limit)

  override def getAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, start: Int, limit: Int): Future[List[JsObject]] = advertisementRepo.getAdvertisers(name, location, status, startDate, endDate, start, limit)

  override def getAdvertiserById(advertiserId: BSONObjectID, location: String, startDate: Long, expireDate: Long): Future[List[JsObject]] = advertisementRepo.getAdvertiserById(advertiserId, location, startDate, expireDate)

  override def getDisplayAdvertisements: Future[List[JsObject]] = advertisementRepo.getDisplayAdvertisements
}