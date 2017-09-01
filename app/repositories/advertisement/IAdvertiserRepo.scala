package repositories.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.Advertiser
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.play.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[AdvertiserRepo])
trait IAdvertiserRepo {

  def insertAdvertiser(advertiser: Advertiser): Future[WriteResult]
  def updateAdvertiser(advertiser: Advertiser): Future[WriteResult]
  def deleteAdvertiser(id: String): Future[WriteResult]
  def blockAdvertiser(id: String, status: Int): Future[WriteResult]
  def getAdvertiserById(id: String): Future[Option[Advertiser]]
  def uploadAdvertiserImage(id: String, image: String): Future[WriteResult]
}

class AdvertiserRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends IAdvertiserRepo {

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("advertiser_tbl"))

  override def insertAdvertiser(advertiser: Advertiser): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(advertiser))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new advertiser with result $writeResult")
    }
    writeRes
  }

  override def updateAdvertiser(advertiser: Advertiser): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> advertiser._id.get),
      BSONDocument(
        "$set" -> BSONDocument(
          "name" -> advertiser.name,
          "description" -> advertiser.description,
          "phones" -> advertiser.phones,
          "email" -> advertiser.email,
          "city" -> advertiser.city,
          "address" -> advertiser.address,
          "url" -> advertiser.url
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update advertiser with result $data")
    }
    writeRes
  }

  override def deleteAdvertiser(id: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.remove(Json.obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful delete advertiser with result $data")
    }
    writeRes
  }

  override def blockAdvertiser(id: String, status: Int): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> status
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful block advertiser with result $data")
    }
    writeRes
  }

  override def getAdvertiserById(id: String): Future[Option[Advertiser]] = {
    val advertiser = collection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get)).one[Advertiser])
    advertiser.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertiser by id with result $data")
    }
    advertiser
  }

  override def uploadAdvertiserImage(id: String, image: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "image" -> image
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful upload advertiser's image with result $data")
    }
    writeRes
  }
}
