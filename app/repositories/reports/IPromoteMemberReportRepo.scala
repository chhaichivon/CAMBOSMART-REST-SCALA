package repositories.reports

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.{ PromoteUserPackage, PromoteUser }
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
import utils.FormatDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
/**
 * Created by Ky Sona on 4/18/2017.
 */
@ImplementedBy(classOf[PromoteMemberReportRepo])
trait IPromoteMemberReportRepo {
  def listGrandTotalPromoteMemberIncome(startDate: String, endDate: String): Future[List[JsObject]]
  def listDetailPromoteMemberIncome(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]
}

class PromoteMemberReportRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IPromoteMemberReportRepo {
  def packageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_user_package_tbl"))
  def promoteCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promote_user_tbl"))

  override def listGrandTotalPromoteMemberIncome(startDate: String, endDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val income = promoteCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "userId" -> 1,
          "startDate" -> 1,
          "endDate" -> 1,
          "status" -> 1,
          "duration" -> 1,
          "price" -> 1
        )
      ),
      List(
        Match(Json.obj(
          "status" -> 1,
          "startDate" -> Json.obj("$gte" -> FormatDate.parseDate(startDate).getMillis, "$lte" -> FormatDate.parseDate(endDate).getMillis)
        )),
        Group(
          Json.obj(
            "duration" -> "$duration",
            "price" -> "$price"
          )
        )("users" -> PushField("userId"))
      )
    ).map(_.head[JsObject]))
    income.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get income promote")
    }
    income
    //    val income = packageCollection.flatMap(_.aggregate(
    //      Lookup(
    //        "promote_user_tbl",
    //        "_id",
    //        "packageId",
    //        "promote"
    //      ),
    //      List(
    //        UnwindField("promote"),
    //        Project(
    //          Json.obj(
    //            "promote" -> 1,
    //            "status" -> 1,
    //            "duration" -> 1,
    //            "price" -> 1
    //          )
    //        ),
    //        Match(matchFilter(startDate, endDate, duration, price)),
    //        Group(
    //          Json.obj(
    //            "duration" -> "$duration",
    //            "price" -> "$price"
    //          )
    //        )("users" -> PushField("promote.userId")),
    //        Project(
    //          Json.obj(
    //            "_id" -> 0,
    //            "duration" -> "$_id.duration",
    //            "price" -> "$_id.price",
    //            "users" -> "$users"
    //          )
    //        )
    //      )
    //    ).map(_.head[JsObject]))
    //    income.onComplete {
    //      case Failure(e) => e.printStackTrace()
    //      case Success(data) => println("Successful get promote")
    //    }
    //    income
  }

  /* helper function */
  def matchFilterRequest(name: String, location: String, fromDate: String, toDate: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis
    val toDateLong = FormatDate.parseDate(toDate).getMillis

    if (name != "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> 1
      )
    } else if (name != "" && location != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name Location")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "status" -> 1
      )
    } else if (name != "" && (fromDateLong > 0 && toDateLong > 0) && location == "") {
      println("Name Date")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 1
      )
    } else if (location != "" && name == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Location")
      Json.obj(
        "users.city" -> location,
        "status" -> 1
      )
    } else if (location != "" && (fromDateLong > 0 && toDateLong > 0) && name == "") {
      println("Location Date")
      Json.obj(
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 1
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && location == "" && name == "") {
      println("Date")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 1
      )
    } else if (name != "" && location != "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Name Location Date")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 1
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "status" -> 1
      )
    }
  }

  def getCountRequest(name: String, location: String, startDate: String, endDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        //        Lookup(
        //          "promoted_user_package_tbl",
        //          "packageId",
        //          "_id",
        //          "packages"
        //        ),
        Project(
          Json.obj(
            "status" -> 1,
            "startDate" -> 1,
            "users.userName" -> 1,
            "users.city" -> 1
          //            "packages.duration" -> 1,
          //            "packages.price" -> 1
          )
        ),
        Match(matchFilterRequest(name, location, startDate, endDate)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listDetailPromoteMemberIncome(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountRequest(name, location, startDate, endDate), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("total : " + total)
    val income = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        //        Lookup(
        //          "promoted_user_package_tbl",
        //          "packageId",
        //          "_id",
        //          "packages"
        //        ),
        Project(
          Json.obj(
            "userId" -> 1,
            "packageId" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users.userName" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.city" -> 1,
            "users.profileImage" -> 1,
            "users.dateJoin" -> 1
          //            "packages.duration" -> 1,
          //            "packages.price" -> 1
          )
        ),
        Match(matchFilterRequest(name, location, startDate, endDate)),
        Skip((page * limit) - limit),
        Limit(limit),
        Project(
          Json.obj(
            "userId" -> 1,
            "packageId" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users.userName" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.city" -> 1,
            "users.profileImage" -> 1,
            "users.dateJoin" -> 1,
            //            "packages.duration" -> 1,
            //            "packages.price" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    income.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get incomes")
    }
    income
  }

}
