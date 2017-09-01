package services.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.{ ProductView, Viewer }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import repositories.view.ProductViewRepo

import scala.concurrent.Future

@ImplementedBy(classOf[ProductViewService])
trait IProductViewService {
  def insertView(view: ProductView): Future[WriteResult]
  def updateViewByProductId(productId: String, viewer: Viewer): Future[WriteResult]
  def increaseViews(productId: String, ipAddress: String): Future[WriteResult]
  def clearView(productId: String): Future[WriteResult]
  def getViewByProductId(productId: String, ipAddress: String): Future[List[JsObject]]
  def deleteView(productId: String): Future[WriteResult]
}

class ProductViewService @Inject() (val viewRepo: ProductViewRepo) extends IProductViewService {

  override def insertView(view: ProductView): Future[WriteResult] = viewRepo.insertView(view)

  override def updateViewByProductId(productId: String, viewer: Viewer): Future[WriteResult] = viewRepo.updateViewByProductId(productId, viewer)

  override def increaseViews(productId: String, ipAddress: String): Future[WriteResult] = viewRepo.increaseViews(productId, ipAddress)

  override def clearView(productId: String): Future[WriteResult] = viewRepo.clearView(productId)

  override def getViewByProductId(productId: String, ipAddress: String): Future[List[JsObject]] = viewRepo.getViewByProductId(productId, ipAddress)

  override def deleteView(productId: String): Future[WriteResult] = viewRepo.deleteView(productId)
}
