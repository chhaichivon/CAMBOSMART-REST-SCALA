package repositories.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.VisitorView
import org.joda.time.DateTime
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection
import utils.FormatDate
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[VisitorViewRepo])
trait IVisitorViewRepo {
  def insertVisitorView(view: VisitorView): Future[WriteResult]
  def getVisitorViews(ipAddress: String, fromDate: String, toDate: String): Future[List[VisitorView]]
  def getFilterVisitors(year: Int, month: Int, day: Int): Future[List[JsObject]]
}

class VisitorViewRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends IVisitorViewRepo {

  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("visitor_view_tbl"))

  override def insertVisitorView(view: VisitorView): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(view))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new visitor view with result $writeResult")
    }
    writeRes
  }

  override def getVisitorViews(ipAddress: String, fromDate: String, toDate: String): Future[List[VisitorView]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._

    def filter: JsObject = {
      if (ipAddress != "") {
        /*===============Get visitor by ip======================*/
        println("IP")
        Json.obj("ip" -> ipAddress)
      } else {
        if (FormatDate.parseDate(fromDate).getMillis > 0 && FormatDate.parseDate(toDate).getMillis > 0) {
          /*===============Get visitor by date======================*/
          println("Date")
          Json.obj(
            "year" -> Json.obj("$gte" -> FormatDate.parseDate(fromDate).getYear, "$lte" -> FormatDate.parseDate(toDate).getYear),
            "month" -> Json.obj("$gte" -> FormatDate.parseDate(fromDate).getMonthOfYear, "$lte" -> FormatDate.parseDate(toDate).getMonthOfYear),
            "day" -> Json.obj("$gte" -> FormatDate.parseDate(fromDate).getDayOfMonth, "$lte" -> FormatDate.parseDate(toDate).getDayOfMonth)
          )
        } else {
          /*===============Get visitor Today======================*/
          println("Default")
          val current = new DateTime()
          println(current.getYear, current.getMonthOfYear, current.getDayOfMonth, current.getDayOfYear)
          Json.obj(
            "year" -> current.getYear,
            "month" -> current.getMonthOfYear,
            "day" -> current.getDayOfMonth
          )
        }
      }
    }
    val visitorViews = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "ip" -> 1,
          "visitedDate" -> 1,
          "year" -> Json.obj("$year" -> "$visitedDate"),
          "month" -> Json.obj("$month" -> "$visitedDate"),
          "day" -> Json.obj("$dayOfMonth" -> "$visitedDate"),
          "amount" -> 1
        )
      ),
      List(
        Match(filter)
      )
    ).map(_.head[VisitorView]))

    /*collection.flatMap(_.find(filter)
    .sort(Json.obj("visitedDate" -> -1))
    .cursor[VisitorView](ReadPreference.primary)
    .collect[List](Int.MaxValue, Cursor.FailOnError[List[VisitorView]]()))*/

    visitorViews.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get visitor view with result $data")
    }
    visitorViews
  }

  override def getFilterVisitors(year: Int, month: Int, day: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      val date = new DateTime()
      if (year > 0 && month <= 0 && day <= 0) {
        Json.obj("year" -> year)
      } else if (year > 0 && month > 0 && day <= 0) {
        Json.obj("year" -> year, "month" -> month)
      } else if (year > 0 && month > 0 && day > 0) {
        Json.obj("year" -> year, "month" -> month, "day" -> day)
      } else {
        println("Default")
        Json.obj("year" -> date.getYear, "month" -> date.getMonthOfYear, "day" -> date.getDayOfMonth)
      }
    }
    val visitors = collection.flatMap(_.aggregate(
      Group(JsString("state"))(
        "all" -> Push(
          Json.obj(
            "ip" -> "$ip",
            "visitedDate" -> "$visitedDate"
          )
        ),
        "total" -> SumAll
      ),
      List(
        UnwindField("all"),
        Project(
          Json.obj(
            "_id" -> 0,
            "ip" -> "$all.ip",
            "visitedDate" -> "$all.visitedDate",
            "year" -> Json.obj("$year" -> "$all.visitedDate"),
            "month" -> Json.obj("$month" -> "$all.visitedDate"),
            "day" -> Json.obj("$dayOfMonth" -> "$all.visitedDate"),
            "total" -> "$total"
          )
        ),
        Match(filter),
        Project(
          Json.obj(
            "_id" -> 0,
            "ip" -> "$ip",
            "total" -> "$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    visitors.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get filter visitor views !!")
    }
    visitors
  }

}