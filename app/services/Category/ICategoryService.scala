package services.category
import com.google.inject.{ ImplementedBy, Inject }
import models.category.Category
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.category.ICategoryRepo

import scala.concurrent.Future

@ImplementedBy(classOf[CategoryService])
trait ICategoryService {

  def insertCategory(category: Category): Future[WriteResult]

  def updateCategory(category: Category): Future[WriteResult]

  def deleteCategory(id: String): Future[WriteResult]

  def getCategoryById(id: String): Future[Option[Category]]

  def getCategoryByName(name: String): Future[Option[Category]]

  def getCategoryByIcon(icon: String): Future[Option[Category]]

  def listParentCategory(page: Int, limit: Int): Future[List[Category]]

  def listSubCategoryById(parentId: String, page: Int, limit: Int): Future[List[Category]]

  def listPopularCategory(limit: Int): Future[List[JsObject]]

  def removeProductId(productId: String): Future[WriteResult]
  /**----------------------------------END---------------------------------------------*/

  def listChildCategoryByParentId(parentId: String, ancestor: Int, page: Int, limit: Int): Future[List[JsObject]]
  def listAllCategory(): Future[List[JsObject]]
  def listParentAndChild(): Future[List[JsObject]]
  def listParentCategoryWithPagination(page: Int, limit: Int): Future[List[JsObject]]
  def filterCategoryByName(name: String): Future[List[JsObject]]
  /*oudam*/
  def updateProductIdInCategory(id: String, productId: List[BSONObjectID]): Future[WriteResult]
  def getThirdCategoryByName(name: String): Future[List[JsObject]]
  /*naseat*/
  def checkSubCategory(name: String): Future[List[JsObject]]

}

class CategoryService @Inject() (val categoryRepo: ICategoryRepo) extends ICategoryService {

  override def insertCategory(category: Category): Future[WriteResult] = categoryRepo.insertCategory(category)

  override def updateCategory(category: Category): Future[WriteResult] = categoryRepo.updateCategory(category)

  override def deleteCategory(id: String): Future[WriteResult] = categoryRepo.deleteCategory(id)

  override def getCategoryById(id: String): Future[Option[Category]] = categoryRepo.getCategoryById(id)

  override def getCategoryByName(name: String): Future[Option[Category]] = categoryRepo.getCategoryByName(name)

  override def getCategoryByIcon(icon: String): Future[Option[Category]] = categoryRepo.getCategoryByIcon(icon)

  override def listParentCategory(page: Int, limit: Int): Future[List[Category]] = categoryRepo.listParentCategory(page, limit)

  override def listSubCategoryById(parentId: String, page: Int, limit: Int): Future[List[Category]] = categoryRepo.listSubCategoryById(parentId, page, limit)

  override def listPopularCategory(limit: Int): Future[List[JsObject]] = categoryRepo.listPopularCategory(limit)

  override def removeProductId(productId: String): Future[WriteResult] = categoryRepo.removeProductId(productId)

  /** ----------------------------------END--------------------------------------------- */
  override def listChildCategoryByParentId(parentId: String, ancestor: Int, page: Int, limit: Int): Future[List[JsObject]] = categoryRepo.listChildCategoryByParentId(parentId, ancestor, page, limit)

  override def listAllCategory(): Future[List[JsObject]] = categoryRepo.listAllCategory()

  override def listParentAndChild(): Future[List[JsObject]] = categoryRepo.listParentAndChild()

  override def listParentCategoryWithPagination(page: Int, limit: Int): Future[List[JsObject]] = categoryRepo.listParentCategoryWithPagination(page, limit)

  override def filterCategoryByName(name: String): Future[List[JsObject]] = categoryRepo.filterCategoryByName(name)

  /*oudam*/
  override def updateProductIdInCategory(id: String, productId: List[BSONObjectID]): Future[WriteResult] = categoryRepo.updateProductIdInCategory(id, productId)

  override def getThirdCategoryByName(name: String): Future[List[JsObject]] = categoryRepo.getThirdCategoryByName(name)

  /*naseat*/
  override def checkSubCategory(name: String): Future[List[JsObject]] = categoryRepo.checkSubCategory(name)
}
