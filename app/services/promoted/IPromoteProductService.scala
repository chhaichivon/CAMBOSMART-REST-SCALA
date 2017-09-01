package services.promoted

import com.google.inject.{ Inject, ImplementedBy }
import models.promoted.PromoteProduct
import play.api.libs.json.JsObject
import models.store.Store
import models.product.Product
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.promoted.{ IPromoteProductRepo }
import scala.concurrent.Future

/**
 * Created by Ky Sona on 3/9/2017.
 */
@ImplementedBy(classOf[PromoteProductService])
trait IPromoteProductService {
  def listUserPromoteProduct(location: String, dateStart: String, dateEnd: String, name: String, page: Int, limit: Int): Future[List[JsObject]]
  def listPromoteProductByUserId(promoteId: String, userId: String): Future[List[JsObject]]
  def approveProductById(ids: List[String], promoteType: String): Future[WriteResult]
  def approvedPromotedRequestById(id: String): Future[WriteResult]
  def deleteUserRequestById(id: String): Future[WriteResult]
  def deleteProductRequestById(productId: String): Future[WriteResult]
  def updatePriceById(id: String, price: Double): Future[WriteResult]
  def insertPromoteProducts(promoted: PromoteProduct): Future[WriteResult]
  def listPromotedProductsExpired(page: Int, limit: Int): Future[List[JsObject]]
  def updatePromotedProductExpired(): Future[List[JsObject]]
}

class PromoteProductService @Inject() (val promoteProductRepo: IPromoteProductRepo) extends IPromoteProductService {
  override def listUserPromoteProduct(location: String, dateStart: String, dateEnd: String, name: String, page: Int, limit: Int): Future[List[JsObject]] = promoteProductRepo.listUserPromoteProduct(location, dateStart, dateEnd, name, page, limit)

  override def listPromoteProductByUserId(promoteId: String, userId: String): Future[List[JsObject]] = promoteProductRepo.listPromoteProductByUserId(promoteId, userId)

  override def approveProductById(ids: List[String], promoteType: String): Future[WriteResult] = promoteProductRepo.approveProductById(ids, promoteType)

  override def approvedPromotedRequestById(id: String): Future[WriteResult] = promoteProductRepo.approvedPromotedRequestById(id)

  override def deleteUserRequestById(id: String): Future[WriteResult] = promoteProductRepo.deleteUserRequestById(id)

  override def deleteProductRequestById(productId: String): Future[WriteResult] = promoteProductRepo.deleteProductRequestById(productId)

  override def updatePriceById(id: String, price: Double): Future[WriteResult] = promoteProductRepo.updatePriceById(id, price)

  override def insertPromoteProducts(promoted: PromoteProduct): Future[WriteResult] = promoteProductRepo.insertPromoteProducts(promoted)

  override def updatePromotedProductExpired(): Future[List[JsObject]] = promoteProductRepo.updatePromotedProductExpired()

  override def listPromotedProductsExpired(page: Int, limit: Int): Future[List[JsObject]] = promoteProductRepo.listPromotedProductsExpired(page, limit)
}
