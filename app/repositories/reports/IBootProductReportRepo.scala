package repositories.reports

import javax.inject.Inject
import com.google.inject.ImplementedBy
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
 * Created by Ky Sona on 4/5/2017.
 */

@ImplementedBy(classOf[BootProductReportRepo])
trait IBootProductReportRepo {
  def listBootProductIncomeGrand(startDate: String, endDate: String): Future[List[JsObject]]
  def listBootProductIncomeDetail(startDate: String, endDate: String, userType: String, page: Int, limit: Int): Future[List[JsObject]]
}

class BootProductReportRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IBootProductReportRepo {
  def packageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_package_tbl"))
  def promoteCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_product_tbl"))
  def userCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))

  override def listBootProductIncomeGrand(startDate: String, endDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val fromDateLong = FormatDate.parseDate(startDate).getMillis
    val toDateLong = FormatDate.parseDate(endDate).getMillis

    val income = promoteCollection.flatMap(_.aggregate(
      UnwindField("productId"),
      List(
        Project(
          Json.obj(
            "status" -> 1,
            "startDate" -> 1,
            "userId" -> 1,
            "typePromote" -> 1,
            "duration" -> 1,
            "price" -> 1
          )
        ),
        Match(
          Json.obj(
            "status" -> 1,
            "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
          )
        ),
        Group(
          Json.obj(
            "type" -> "$typePromote",
            "duration" -> "$duration",
            "price" -> "$price"
          )
        )(
            "totalProducts" -> SumAll,
            "users" -> PushField("userId")
          ),
        Project(
          Json.obj(
            "type" -> "$type",
            "duration" -> "$duration",
            "price" -> "$price",
            "totalProducts" -> "$totalProducts",
            "users" -> "$users"
          )
        )
      )
    ).map(_.head[JsObject]))
    income.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get promoted products income")
    }
    income
  }

  def matchFilterByUser(dateStart: String, dateEnd: String, userType: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(dateStart).getMillis
    val toDateLong = FormatDate.parseDate(dateEnd).getMillis

    if ((fromDateLong > 0 && toDateLong > 0) && userType == "") {
      println("Date")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 1
      )
    } else if ((fromDateLong < 0 && toDateLong < 0) && userType != "") {
      println("Type")
      Json.obj(
        "user.userType" -> userType,
        "status" -> 1
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && userType != "") {
      println("Date and Type")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "user.userType" -> userType,
        "status" -> 1
      )
    } else {
      println("Default is Status")
      Json.obj(
        "status" -> 1
      )
    }
  }

  def getCountUsers(dateStart: String, dateEnd: String, promoteType: String, duration: Int, price: Double, userType: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = promoteCollection.flatMap(_.aggregate(
      UnwindField("productId"),
      List(
        Lookup(
          "user_tbl",
          "userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        Project(
          Json.obj(
            "status" -> 1,
            "startDate" -> 1,
            "typePromote" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "user.userType" -> 1,
            "user.userName" -> 1
          )
        ),
        Match(matchFilterByUser(dateStart, dateEnd, userType)),
        Group(
          Json.obj(
            "userName" -> "$user.userName",
            "userType" -> "$user.userType",
            "type" -> "$typePromote",
            "duration" -> "$duration",
            "price" -> "$price"
          )
        )(
            "totalUser" -> SumAll
          )
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listBootProductIncomeDetail(startDate: String, endDate: String, userType: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    //    val jsObj: List[JsObject] = Await.result(getCountUsers(startDate, endDate, promoteType, duration, price, userType), 10.seconds)
    //    var total: Int = 0
    //    if (jsObj.nonEmpty) {
    //      total = (jsObj.head \ "total").as[Int]
    //    } else {
    //      total = 0
    //    }
    //    println("Total Product Count" + total)
    val income = promoteCollection.flatMap(_.aggregate(
      UnwindField("productId"),
      List(
        Lookup(
          "user_tbl",
          "userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        Project(
          Json.obj(
            "status" -> 1,
            "startDate" -> 1,
            "typePromote" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "user.userType" -> 1,
            "user.userName" -> 1
          )
        ),
        Match(matchFilterByUser(startDate, endDate, userType)),
        Group(
          Json.obj(
            "userName" -> "$user.userName",
            "userType" -> "$user.userType",
            "type" -> "$typePromote",
            "duration" -> "$duration",
            "price" -> "$price"
          )
        )(
            "totalProducts" -> SumAll

          ),
        Project(
          Json.obj(
            "userName" -> "$userName",
            "userType" -> "$userType",
            "totalProducts" -> "$totalProducts"
          )
        ),
        Skip((page * limit) - limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    income.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get promoted products incomes")
    }
    income
  }

}
