package repositories.subscribe

import com.google.inject.{ ImplementedBy, Inject }
import models.subscribe.Subscribe
import play.api.libs.json.{ JsObject, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[SubscribeRepo])
trait ISubscribeRepo {
  def insertSubscribe(subscribe: Subscribe): Future[WriteResult]
  def getSubscribeByStoreIdAndUserId(storeId: BSONObjectID, userId: BSONObjectID): Future[Option[JsObject]]
  def deleteSubscribe(id: BSONObjectID): Future[WriteResult]
  def getUserHasSubscribeByStoreId(storeId: BSONObjectID): Future[List[JsObject]]
}

class SubscribeRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends ISubscribeRepo {
  def subscribeCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("subscribe_tbl"))

  override def insertSubscribe(subscribe: Subscribe): Future[WriteResult] = {
    val writeResult = subscribeCollection.flatMap(_.insert(subscribe))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful insert new subscribe with result $data")
    }
    writeResult
  }

  override def getSubscribeByStoreIdAndUserId(storeId: BSONObjectID, userId: BSONObjectID): Future[Option[JsObject]] = {
    val subscribe = subscribeCollection.flatMap(_.find(
      Json.obj(
        "userId" -> userId,
        "storeId" -> storeId
      )
    ).one[JsObject])
    subscribe.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get subscribe with result $data")
    }
    subscribe
  }

  override def deleteSubscribe(id: BSONObjectID): Future[WriteResult] = {
    val writeResult = subscribeCollection.flatMap(_.remove(Json.obj("_id" -> id)))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful delete subscribe with result $data")
    }
    writeResult
  }

  override def getUserHasSubscribeByStoreId(storeId: BSONObjectID): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val subscribe = subscribeCollection.flatMap(_.aggregate(
      Match(Json.obj(
        "storeId" -> storeId
      )),
      List(
        Group(Json.obj(
          "storeId" -> "$storeId"
        ))("users" -> Push(Json.obj(
          "userId" -> "$userId",
          "email" -> "$email"
        )))
      )
    ).map(_.head[JsObject]))
    subscribe.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get users subscribe store with result $data")
    }
    subscribe
  }
}