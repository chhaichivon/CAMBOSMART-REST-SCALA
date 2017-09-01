package services.notification

import com.google.inject.{ ImplementedBy, Inject }
import models.notification.Notifications
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.notification.INotificationRepo

import scala.concurrent.Future

@ImplementedBy(classOf[NotificationService])
trait INotificationService {
  def insertNotification(notifications: Notifications): Future[WriteResult]
  def getAllNotificationsByUserId(userId: BSONObjectID, limit: Int): Future[List[JsObject]]
  def updateAllViewNotification(userId: BSONObjectID): Future[WriteResult]
  def updateDirtyNotification(id: BSONObjectID): Future[WriteResult]
  def countNotificationUnView(userId: BSONObjectID): Future[List[JsObject]]
  def getNotificationById(id: BSONObjectID): Future[Option[Notifications]]
}

class NotificationService @Inject() (val notificationRepo: INotificationRepo) extends INotificationService {
  override def insertNotification(notifications: Notifications): Future[WriteResult] = notificationRepo.insertNotification(notifications)

  override def getAllNotificationsByUserId(userId: BSONObjectID, limit: Int): Future[List[JsObject]] = notificationRepo.getAllNotificationsByUserId(userId, limit)

  override def updateAllViewNotification(userId: BSONObjectID): Future[WriteResult] = notificationRepo.updateAllViewNotification(userId)

  override def updateDirtyNotification(id: BSONObjectID): Future[WriteResult] = notificationRepo.updateDirtyNotification(id)

  override def countNotificationUnView(userId: BSONObjectID): Future[List[JsObject]] = notificationRepo.countNotificationUnView(userId)

  override def getNotificationById(id: BSONObjectID): Future[Option[Notifications]] = notificationRepo.getNotificationById(id)
}
