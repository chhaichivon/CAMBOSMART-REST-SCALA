package services.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.Advertiser
import reactivemongo.api.commands.WriteResult
import repositories.advertisement.IAdvertiserRepo

import scala.concurrent.Future

@ImplementedBy(classOf[AdvertiserService])
trait IAdvertiserService {
  def insertAdvertiser(advertiser: Advertiser): Future[WriteResult]
  def updateAdvertiser(advertiser: Advertiser): Future[WriteResult]
  def deleteAdvertiser(id: String): Future[WriteResult]
  def blockAdvertiser(id: String, status: Int): Future[WriteResult]
  def getAdvertiserById(id: String): Future[Option[Advertiser]]
  def uploadAdvertiserImage(id: String, image: String): Future[WriteResult]
}

class AdvertiserService @Inject() (val advertiserRepo: IAdvertiserRepo) extends IAdvertiserService {

  override def insertAdvertiser(advertiser: Advertiser): Future[WriteResult] = advertiserRepo.insertAdvertiser(advertiser)

  override def updateAdvertiser(advertiser: Advertiser): Future[WriteResult] = advertiserRepo.updateAdvertiser(advertiser)

  override def deleteAdvertiser(id: String): Future[WriteResult] = ???

  override def blockAdvertiser(id: String, status: Int): Future[WriteResult] = advertiserRepo.blockAdvertiser(id, status)

  override def getAdvertiserById(id: String): Future[Option[Advertiser]] = advertiserRepo.getAdvertiserById(id)

  override def uploadAdvertiserImage(id: String, image: String): Future[WriteResult] = advertiserRepo.uploadAdvertiserImage(id, image)
}