package repositories.promoted

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.{ PromoteUserPackage }
import play.api.libs.json.Json._
import play.api.libs.json.{ Json, JsObject, JsString }
import reactivemongo.api.commands.bson.BSONDistinctCommand
import reactivemongo.api.{ QueryOpts, Cursor, ReadPreference }
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson
import reactivemongo.bson.{ BSONString, BSONDocument, BSONObjectID }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
/**
 * Created by Ky Sona on 4/7/2017.
 */
@ImplementedBy(classOf[PromoteUserPackageRepo])
trait IPromoteUserPackageRepo {
  def insertUserPromotePackage(promoteUserPackage: PromoteUserPackage): Future[WriteResult]
  def listUserPromotePackage(page: Int, limit: Int): Future[List[JsObject]]
  def getUserPromotePackageById(id: String): Future[Option[PromoteUserPackage]]
  def updateUserPromotePackageById(promoteUserPackage: PromoteUserPackage): Future[WriteResult]
  def deleteUserPromotePackageById(id: String): Future[WriteResult]
  def listAllUserPromotePackages(): Future[List[JsObject]]
}

class PromoteUserPackageRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IPromoteUserPackageRepo {
  def userPackageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_user_package_tbl"))

  override def insertUserPromotePackage(promoteUserPackage: PromoteUserPackage): Future[WriteResult] = {
    val writeRes = userPackageCollection.flatMap(_.insert(promoteUserPackage))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new promoted user package")
    }
    writeRes
  }
  /* helper function */
  def getCountUserPackages(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = userPackageCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "status" -> 1
        )
      ),
      List(
        Match(Json.obj("status" -> 1)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listUserPromotePackage(page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountUserPackages(), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Package Count" + total)
    val skip = (page * limit) - limit
    val packages = userPackageCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "duration" -> 1,
          "price" -> 1,
          "description" -> 1,
          "status" -> 1,
          "total" -> s"$total"
        )
      ),
      List(
        Match(Json.obj("status" -> 1)),
        Skip(skip),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully list promoted user package")
    }
    packages
  }

  override def listAllUserPromotePackages(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val packages = userPackageCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "duration" -> 1,
          "price" -> 1,
          "description" -> 1,
          "status" -> 1
        )
      ),
      List(
        Match(Json.obj("status" -> 1))
      )
    ).map(_.head[JsObject]))
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully list all promoted user package")
    }
    packages
  }

  override def getUserPromotePackageById(id: String): Future[Option[PromoteUserPackage]] = {
    val packages = userPackageCollection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get, "status" -> 1))
      .one[PromoteUserPackage])
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully found promoted user package by ID")
    }
    packages
  }

  override def updateUserPromotePackageById(promoteUserPackage: PromoteUserPackage): Future[WriteResult] = {
    val writeRes = userPackageCollection.flatMap(_.update(
      Json.obj("_id" -> promoteUserPackage._id),
      BSONDocument(
        "$set" -> BSONDocument(
          "duration" -> promoteUserPackage.duration,
          "price" -> promoteUserPackage.price,
          "description" -> promoteUserPackage.description
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update promoted user package")
    }
    writeRes
  }

  override def deleteUserPromotePackageById(id: String): Future[WriteResult] = {
    val writeRes = userPackageCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument("status" -> 0)
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully deleted promoted user package by ID")
    }
    writeRes
  }

}
