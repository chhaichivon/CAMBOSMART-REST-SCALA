package repositories.promoted

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.{ PromoteProductPackage }
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
 * Created by Ky Sona on 3/15/2017.
 */
@ImplementedBy(classOf[PromoteProductPackageRepo])
trait IPromoteProductPackageRepo {
  def listPromotePackage(typePromote: String, page: Int, limit: Int): Future[List[JsObject]]
  def insertPromotePackage(promotePackage: PromoteProductPackage): Future[WriteResult]
  def getPromotePackageById(id: String): Future[Option[PromoteProductPackage]]
  def updatePromotePackageById(promotePackage: PromoteProductPackage): Future[WriteResult]
  def deletePromotePackageById(id: String): Future[WriteResult]
  def listAllPackages(): Future[List[JsObject]]
}

class PromoteProductPackageRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IPromoteProductPackageRepo {
  def packageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_package_tbl"))

  /* Helper Function */
  def getCountPackages(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = packageCollection.flatMap(_.aggregate(
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

  def matchFilter(typePromote: String): JsObject = {
    if (typePromote == "") {
      println("status")
      Json.obj(
        "status" -> 1
      )
    } else {
      println("type status")
      Json.obj(
        "typePromote" -> typePromote,
        "status" -> 1
      )
    }
  }
  override def listPromotePackage(typePromote: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountPackages(), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Package Count" + total)
    val skip = (page * limit) - limit
    val packages = packageCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "typePromote" -> 1,
          "duration" -> 1,
          "price" -> 1,
          "description" -> 1,
          "status" -> 1,
          "createDate" -> 1,
          "total" -> s"$total"
        )
      ),
      List(
        Match(matchFilter(typePromote)),
        Sort(Descending("createDate")),
        Skip(skip),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully list promoted package")
    }
    packages
  }

  override def insertPromotePackage(promotePackage: PromoteProductPackage): Future[WriteResult] = {
    val writeRes = packageCollection.flatMap(_.insert(promotePackage))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new promoted package")
    }
    writeRes
  }

  override def getPromotePackageById(id: String): Future[Option[PromoteProductPackage]] = {
    val packages = packageCollection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get, "status" -> 1))
      .one[PromoteProductPackage])
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully found promoted package by ID")
    }
    packages
  }

  override def updatePromotePackageById(promotePackage: PromoteProductPackage): Future[WriteResult] = {
    val writeRes = packageCollection.flatMap(_.update(
      Json.obj("_id" -> promotePackage._id),
      BSONDocument(
        "$set" -> BSONDocument(
          "typePromote" -> promotePackage.typePromote,
          "duration" -> promotePackage.duration,
          "price" -> promotePackage.price,
          "description" -> promotePackage.description
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update promoted package")
    }
    writeRes
  }

  override def deletePromotePackageById(id: String): Future[WriteResult] = {
    val writeRes = packageCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument("status" -> 0)
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully deleted promoted package by ID")
    }
    writeRes
  }

  override def listAllPackages(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val packages = packageCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "typePromote" -> 1,
          "duration" -> 1,
          "price" -> 1,
          "status" -> 1
        )
      ),
      List(
        Match(Json.obj("status" -> 1))
      )
    ).map(_.head[JsObject]))
    packages.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully list all promoted package")
    }
    packages
  }

}
