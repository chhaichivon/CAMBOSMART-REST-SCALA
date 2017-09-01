package services.store

import com.google.inject.{ ImplementedBy, Inject }
import models.store.Store
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.store.IStoreRepo

import scala.concurrent.Future

@ImplementedBy(classOf[StoreService])
trait IStoreService {
  def updateStore(store: Store): Future[WriteResult]

  def deleteStore(id: String): Future[WriteResult]

  def getStoreByUserId(id: String): Future[Option[Store]]

  def updateProductIdInStore(id: String, productId: List[BSONObjectID]): Future[WriteResult]

  def updateStoreByUserId(userId: String, storeName: String, storeInformation: String): Future[WriteResult]

  def updateStoreMapByUserId(userId: String, latitude: Double, longitude: Double): Future[WriteResult]

  def uploadStoreBanner(userId: String, storeBanner: String): Future[WriteResult]

  def removeProductId(productId: String): Future[WriteResult]
}

class StoreService @Inject() (val storeRepo: IStoreRepo) extends IStoreService {
  override def updateStore(store: Store): Future[WriteResult] = storeRepo.updateStore(store)

  override def deleteStore(id: String): Future[WriteResult] = storeRepo.deleteStore(id)

  override def getStoreByUserId(id: String): Future[Option[Store]] = storeRepo.getStoreByUserId(id)

  override def updateProductIdInStore(id: String, productId: List[BSONObjectID]): Future[WriteResult] = storeRepo.updateProductIdInStore(id, productId)

  override def updateStoreByUserId(userId: String, storeName: String, storeInformation: String): Future[WriteResult] = storeRepo.updateStoreByUserId(userId, storeName, storeInformation)

  override def updateStoreMapByUserId(userId: String, latitude: Double, longitude: Double): Future[WriteResult] = storeRepo.updateStoreMapByUserId(userId, latitude, longitude)

  override def uploadStoreBanner(userId: String, storeBanner: String): Future[WriteResult] = storeRepo.uploadStoreBanner(userId, storeBanner)

  override def removeProductId(productId: String): Future[WriteResult] = storeRepo.removeProductId(productId)
}
