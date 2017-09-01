package repositories.notification

import com.google.inject.{ ImplementedBy, Inject }
import models.notification.Notifications
import play.api.libs.json.{ JsObject, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[NotificationRepo])
trait INotificationRepo {
  def insertNotification(notifications: Notifications): Future[WriteResult]
  def getAllNotificationsByUserId(userId: BSONObjectID, limit: Int): Future[List[JsObject]]
  def updateAllViewNotification(userId: BSONObjectID): Future[WriteResult]
  def updateDirtyNotification(id: BSONObjectID): Future[WriteResult]
  def countNotificationUnView(userId: BSONObjectID): Future[List[JsObject]]
  def getNotificationById(id: BSONObjectID): Future[Option[Notifications]]
}

class NotificationRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends INotificationRepo {
  def notificationCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("notification_tbl"))

  override def insertNotification(notifications: Notifications): Future[WriteResult] = {
    val writeResult = notificationCollection.flatMap(_.insert(notifications))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful insert new notification with result $data")
    }
    writeResult
  }

  override def getAllNotificationsByUserId(userId: BSONObjectID, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val notification = notificationCollection.flatMap(_.aggregate(
      Match(Json.obj(
        "userId" -> userId
      )),
      List(
        Sort(Descending("date")),
        Limit(limit),
        Group(Json.obj(
          "userId" -> "$userId"
        ))("detail" -> Push(Json.obj(
          "_id" -> "$_id",
          "type" -> "$notificationType",
          "description" -> "$description",
          "isView" -> "$isView",
          "isDirty" -> "$isDirty",
          "date" -> "$date"
        )))
      )
    ).map(_.head[JsObject]))
    notification.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get all notifications by user with result $data")
    }
    notification
  }

  override def updateAllViewNotification(userId: BSONObjectID): Future[WriteResult] = {
    val writeResult = notificationCollection.flatMap(_.update(
      Json.obj(
        "userId" -> userId,
        "isView" -> false
      ),
      BSONDocument(
        "$set" -> BSONDocument(
          "isView" -> true
        )
      ),
      multi = true
    ))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update view notification with result $data")
    }
    writeResult
  }

  override def updateDirtyNotification(id: BSONObjectID): Future[WriteResult] = {
    val writeResult = notificationCollection.flatMap(_.update(
      Json.obj("_id" -> id),
      BSONDocument(
        "$set" -> BSONDocument(
          "isDirty" -> true
        )
      )
    ))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update dirty notification with result $data")
    }
    writeResult
  }

  override def countNotificationUnView(userId: BSONObjectID): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val notification = notificationCollection.flatMap(_.aggregate(
      Match(Json.obj(
        "userId" -> userId,
        "isView" -> false
      )),
      List(
        Group(Json.obj(
          "_id" -> "$state"
        ))("count" -> SumAll)
      )
    ).map(_.head[JsObject]))
    notification.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful count notification with result $data")
    }
    notification
  }

  override def getNotificationById(id: BSONObjectID): Future[Option[Notifications]] = {
    val notification = notificationCollection.flatMap(_.find(Json.obj("_id" -> id)).one[Notifications])
    notification.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get notification by id with result $data")
    }
    notification
  }
}