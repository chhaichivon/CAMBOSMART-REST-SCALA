package services.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.{ Advertise, CategoryAdvertisement }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.advertisement.ICategoryAdvertisementRepo

import scala.concurrent.Future

@ImplementedBy(classOf[CategoryAdvertisementService])
trait ICategoryAdvertisementService {

  def insertCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult]

  def updateCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult]

  def deleteCategoryAdvertisement(id: String): Future[WriteResult]

  def getCategoryAdvertisementById(id: String): Future[Option[CategoryAdvertisement]]

  def getCategoryAdvertisementByCategoryId(id: BSONObjectID): Future[Option[CategoryAdvertisement]]

  def getCategoryAdvertisements: Future[List[CategoryAdvertisement]]

  def getCategoryAdvertisementSchedule(id: String): Future[List[JsObject]]

  def updateCategoryAdvertisementInPackage(id: String, advertise: Advertise): Future[WriteResult]

  def getCategoryAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]

  def getCategoryAdvertiser(id: String, startDate: Long, expireDate: Long): Future[List[JsObject]]

  def updateAdvertise(advertiserId: String): Future[WriteResult]

  def getDisplayCategoryAdvertisements: Future[List[JsObject]]
}

class CategoryAdvertisementService @Inject() (val categoryAdvertisementRepo: ICategoryAdvertisementRepo) extends ICategoryAdvertisementService {

  override def insertCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult] = categoryAdvertisementRepo.insertCategoryAdvertisement(categoryAdvertisement)

  override def updateCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult] = categoryAdvertisementRepo.updateCategoryAdvertisement(categoryAdvertisement)

  override def deleteCategoryAdvertisement(id: String): Future[WriteResult] = categoryAdvertisementRepo.deleteCategoryAdvertisement(id)

  override def getCategoryAdvertisementById(id: String): Future[Option[CategoryAdvertisement]] = categoryAdvertisementRepo.getCategoryAdvertisementById(id)

  override def getCategoryAdvertisementByCategoryId(id: BSONObjectID): Future[Option[CategoryAdvertisement]] = categoryAdvertisementRepo.getCategoryAdvertisementByCategoryId(id)

  override def getCategoryAdvertisements: Future[List[CategoryAdvertisement]] = categoryAdvertisementRepo.getCategoryAdvertisements

  override def getCategoryAdvertisementSchedule(id: String): Future[List[JsObject]] = categoryAdvertisementRepo.getCategoryAdvertisementSchedule(id)

  override def updateCategoryAdvertisementInPackage(id: String, advertise: Advertise): Future[WriteResult] = categoryAdvertisementRepo.updateCategoryAdvertisementInPackage(id, advertise)

  override def getCategoryAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] = {
    categoryAdvertisementRepo.getCategoryAdvertisers(name, location, status, startDate, endDate, page, limit)
  }

  override def getCategoryAdvertiser(id: String, startDate: Long, expireDate: Long): Future[List[JsObject]] = categoryAdvertisementRepo.getCategoryAdvertiser(id, startDate, expireDate)

  override def updateAdvertise(advertiserId: String): Future[WriteResult] = categoryAdvertisementRepo.updateAdvertise(advertiserId)

  override def getDisplayCategoryAdvertisements: Future[List[JsObject]] = categoryAdvertisementRepo.getDisplayCategoryAdvertisements
}
