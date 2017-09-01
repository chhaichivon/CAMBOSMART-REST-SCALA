package repositories.store

import com.google.inject.{ ImplementedBy, Inject }
import models.store.Store
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[StoreRepo])
trait IStoreRepo {
  def updateStore(store: Store): Future[WriteResult]

  def deleteStore(id: String): Future[WriteResult]

  def getStoreByUserId(id: String): Future[Option[Store]]

  def updateProductIdInStore(id: String, productId: List[BSONObjectID]): Future[WriteResult]

  def updateStoreByUserId(userId: String, storeName: String, storeInformation: String): Future[WriteResult]

  def updateStoreMapByUserId(userId: String, latitude: Double, longitude: Double): Future[WriteResult]

  def uploadStoreBanner(userId: String, storeBanner: String): Future[WriteResult]

  def removeProductId(productId: String): Future[WriteResult]
}

class StoreRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IStoreRepo {

  def storeCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("store_tbl"))

  override def updateStore(store: Store): Future[WriteResult] = ???

  override def deleteStore(id: String): Future[WriteResult] = ???

  override def getStoreByUserId(id: String): Future[Option[Store]] = {
    val store = storeCollection.flatMap(_.find(Json.obj("userId" -> BSONObjectID.parse(id).get)).one[Store])
    store.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get store by userId with result $data !!")
    }
    store
  }

  override def updateProductIdInStore(id: String, productId: List[BSONObjectID]): Future[WriteResult] = {
    val store = storeCollection.flatMap(_.update(Json.obj("userId" -> BSONObjectID.parse(id).get), BSONDocument("$set" -> BSONDocument("productId" -> productId))))
    store.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update productId in store with result $data !!")
    }
    store
  }

  override def updateStoreByUserId(userId: String, storeName: String, storeInformation: String): Future[WriteResult] = {
    val writeRes = storeCollection.flatMap(_.update(
      Json.obj("userId" -> BSONObjectID.parse(userId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "storeName" -> storeName,
          "storeInformation" -> storeInformation
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update store")
    }
    writeRes
  }

  override def updateStoreMapByUserId(userId: String, latitude: Double, longitude: Double): Future[WriteResult] = {
    val writeRes = storeCollection.flatMap(_.update(
      Json.obj("userId" -> BSONObjectID.parse(userId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "latitude" -> latitude,
          "longitude" -> longitude
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update store")
    }
    writeRes
  }

  override def uploadStoreBanner(userId: String, storeBanner: String): Future[WriteResult] = {
    val writeRes = storeCollection.flatMap(_.update(
      Json.obj("userId" -> BSONObjectID.parse(userId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "storeBanner" -> storeBanner
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Store Banner has been uploaded")
    }
    writeRes
  }

  override def removeProductId(productId: String): Future[WriteResult] = {
    val writeRes = storeCollection.flatMap(_.update(
      Json.obj("productId" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$pull" -> BSONDocument(
          "productId" -> BSONObjectID.parse(productId).get
        )
      ),
      multi = true
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully remove productId from store !!")
    }
    writeRes
  }
}
