package services.advertiser

import com.google.inject.{ ImplementedBy, Inject }
import models.advertiser.{ Advertiser, Advertising }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.advertiser.IAdvertiserRepo

import scala.concurrent.Future

@ImplementedBy(classOf[AdvertiserService])
trait IAdvertiserService {

  def insertAdvertiser(advertiser: Advertiser): Future[WriteResult]

  def insertAdvertising(advertising: Advertising): Future[WriteResult]

  def updateAdvertiser(id: String, advertiser: Advertiser): Future[WriteResult]

  def updateAdvertising(id: String, advertising: Advertising): Future[WriteResult]

  def deleteAdvertiser(id: String, status: Int): Future[WriteResult]

  def getFilterAdvertisers(name: String, city: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]]

  def getAdvertiserById(id: String): Future[Option[JsObject]]

  def getAdvertisingByPageLocation(page: String, location: String, price: Double, dateStart: String, dateEnd: String): Future[Option[JsObject]]

  def updateAdvertiserId(id: String, advertiserId: List[BSONObjectID]): Future[WriteResult]
}

class AdvertiserService @Inject() (advertiserRepo: IAdvertiserRepo) extends IAdvertiserService {

  override def insertAdvertiser(advertiser: Advertiser): Future[WriteResult] = advertiserRepo.insertAdvertiser(advertiser)

  override def insertAdvertising(advertising: Advertising): Future[WriteResult] = advertiserRepo.insertAdvertising(advertising)

  override def updateAdvertiser(id: String, advertiser: Advertiser): Future[WriteResult] = advertiserRepo.updateAdvertiser(id, advertiser)

  override def updateAdvertising(id: String, advertising: Advertising): Future[WriteResult] = advertiserRepo.updateAdvertising(id, advertising)

  override def deleteAdvertiser(id: String, status: Int): Future[WriteResult] = advertiserRepo.deleteAdvertiser(id, status)

  override def getFilterAdvertisers(name: String, city: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]] = {
    advertiserRepo.getFilterAdvertisers(name, city, status, fromDate, toDate, start, limit)
  }

  override def getAdvertiserById(id: String): Future[Option[JsObject]] = advertiserRepo.getAdvertiserById(id)

  override def getAdvertisingByPageLocation(page: String, location: String, price: Double, dateStart: String, dateEnd: String): Future[Option[JsObject]] = advertiserRepo.getAdvertisingByPageLocation(page, location, price, dateStart, dateEnd)

  override def updateAdvertiserId(id: String, advertiserId: List[BSONObjectID]): Future[WriteResult] = advertiserRepo.updateAdvertiserId(id, advertiserId)
}