package services.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.{ CategoryView, Viewer }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.view.ICategoryViewRepo

import scala.concurrent.Future

@ImplementedBy(classOf[CategoryViewService])
trait ICategoryViewService {
  def insertCategoryView(view: CategoryView): Future[WriteResult]
  def updateCategoryViewByCategoryId(categoryId: BSONObjectID, viewer: Viewer): Future[WriteResult]
  def increaseCategoryView(categoryId: BSONObjectID, ipAddress: String): Future[WriteResult]
  def clearCategoryView(categoryId: String): Future[WriteResult]
  def getCategoryViewByIdAndIp(categoryId: BSONObjectID, ipAddress: String): Future[List[JsObject]]
  def getCategoryViewById(categoryId: BSONObjectID): Future[Option[CategoryView]]
}

class CategoryViewService @Inject() (val categoryViewRepo: ICategoryViewRepo) extends ICategoryViewService {

  override def insertCategoryView(view: CategoryView): Future[WriteResult] = categoryViewRepo.insertCategoryView(view)

  override def updateCategoryViewByCategoryId(categoryId: BSONObjectID, viewer: Viewer): Future[WriteResult] = categoryViewRepo.updateCategoryViewByCategoryId(categoryId, viewer)

  override def increaseCategoryView(categoryId: BSONObjectID, ipAddress: String): Future[WriteResult] = categoryViewRepo.increaseCategoryView(categoryId, ipAddress)

  override def clearCategoryView(categoryId: String): Future[WriteResult] = categoryViewRepo.clearCategoryView(categoryId)

  override def getCategoryViewByIdAndIp(categoryId: BSONObjectID, ipAddress: String): Future[List[JsObject]] = categoryViewRepo.getCategoryViewByIdAndIp(categoryId, ipAddress)

  override def getCategoryViewById(categoryId: BSONObjectID): Future[Option[CategoryView]] = categoryViewRepo.getCategoryViewById(categoryId)

}
