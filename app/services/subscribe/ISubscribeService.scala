package services.subscribe

import com.google.inject.{ ImplementedBy, Inject }
import models.subscribe.Subscribe
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.subscribe.ISubscribeRepo

import scala.concurrent.Future

@ImplementedBy(classOf[SubscribeService])
trait ISubscribeService {
  def insertSubscribe(subscribe: Subscribe): Future[WriteResult]
  def getSubscribeByStoreIdAndUserId(storeId: BSONObjectID, userId: BSONObjectID): Future[Option[JsObject]]
  def deleteSubscribe(id: BSONObjectID): Future[WriteResult]
  def getUserHasSubscribeByStoreId(storeId: BSONObjectID): Future[List[JsObject]]
}

class SubscribeService @Inject() (val subscribeRepo: ISubscribeRepo) extends ISubscribeService {
  override def insertSubscribe(subscribe: Subscribe): Future[WriteResult] = subscribeRepo.insertSubscribe(subscribe)

  override def getSubscribeByStoreIdAndUserId(storeId: BSONObjectID, userId: BSONObjectID): Future[Option[JsObject]] = {
    subscribeRepo.getSubscribeByStoreIdAndUserId(storeId, userId)
  }

  override def deleteSubscribe(id: BSONObjectID): Future[WriteResult] = subscribeRepo.deleteSubscribe(id)

  override def getUserHasSubscribeByStoreId(storeId: BSONObjectID): Future[List[JsObject]] = subscribeRepo.getUserHasSubscribeByStoreId(storeId)
}