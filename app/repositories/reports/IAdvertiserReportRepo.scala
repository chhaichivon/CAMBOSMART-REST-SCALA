package repositories.reports

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.{ PromoteUserPackage, PromoteUser }
import org.joda.time.DateTime
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
 * Created by Ky Sona on 4/20/2017.
 */
@ImplementedBy(classOf[AdvertiserReportRepo])
trait IAdvertiserReportRepo {
  def listGrandTotalAdvertiserIncome(page: String, fromDate: String, toDate: String): Future[List[JsObject]]
  def listDetailAdvertiserIncome(page: String, city: String, fromDate: String, toDate: String, name: String, start: Int, limit: Int): Future[List[JsObject]]
  def listCategoryIncome(startDate: String, endDate: String, name: String): Future[List[JsObject]]
}

class AdvertiserReportRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IAdvertiserReportRepo {
  def advertiserCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("advertiser_tbl"))
  def advertisementCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("advertisement_tbl"))
  def categoryCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("category_advertisement_tbl"))

  /* helper function */
  def matchFilterGrand(page: String, fromDate: String, toDate: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis;
    val toDateLong = FormatDate.parseDate(toDate).getMillis;
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;

    if (page != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Page")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*"))
      )
    } else if (page != "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Page Date")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && page == "") {
      println("Date")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      //"expireDate" -> Json.obj("$gte" -> nowLong)
      )
    }
  }

  override def listGrandTotalAdvertiserIncome(page: String, fromDate: String, toDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._

    val advertisements = advertisementCollection.flatMap(_.aggregate(
      UnwindField("advertise"),
      List(
        Lookup(
          "advertiser_tbl",
          "advertise.id",
          "_id",
          "advertisers"
        ),
        UnwindField("advertisers"),
        Project(
          Json.obj(
            "page" -> Json.obj("$toLower" -> "$page"),
            "location" -> 1,
            "price" -> 1,
            "startDate" -> "$advertise.startDate",
            "expireDate" -> "$advertise.expireDate"
          )
        ),
        Match(matchFilterGrand(page, fromDate, toDate)),
        Group(Json.obj("_id" -> "$_id", "page" -> "$page", "location" -> "$location", "price" -> "$price"))(
          "using" -> SumAll
        ),
        Project(
          Json.obj(
            "_id" -> 0,
            "page" -> "$_id.page",
            "location" -> "$_id.location",
            "price" -> "$_id.price",
            "using" -> "$using"
          )
        )
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get advertisements")
    }
    advertisements
  }

  /* helper function */
  def matchFilter(page: String, city: String, fromDate: String, toDate: String, name: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis;
    val toDateLong = FormatDate.parseDate(toDate).getMillis;
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;

    if (page != "" && city == "" && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("Page")
      Json.obj("page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")))
    } else if (page != "" && (fromDateLong > 0 && toDateLong > 0) && city == "" && name == "") {
      println("Page Date")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (page != "" && city != "" && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("Page City")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "city" -> city
      )
    } else if (page != "" && name != "" && (fromDateLong < 0 && toDateLong < 0) && city == "") {
      println("Page Name")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (city != "" && page == "" && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("City")
      Json.obj(
        "city" -> city
      )
    } else if (city != "" && (fromDateLong > 0 && toDateLong > 0) && page == "" && name == "") {
      println("City Date")
      Json.obj(
        "city" -> city,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (city != "" && name != "" && (fromDateLong < 0 && toDateLong < 0) && page == "") {
      println("City Name")
      Json.obj(
        "city" -> city,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && page == "" && city == "" && name == "") {
      println("Date")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && name != "" && page == "" && city == "") {
      println("Date Name")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (name != "" && page == "" && (fromDateLong < 0 && toDateLong < 0) && city == "") {
      println(" Name")
      Json.obj("name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")))
    } else if (page != "" && city != "" && (fromDateLong > 0 && toDateLong > 0) && name == "") {
      println("Page City Date")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "city" -> city,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (page != "" && city != "" && name != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Page City Name")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "city" -> city,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (page != "" && name != "" && (fromDateLong > 0 && toDateLong > 0) && city == "") {
      println("Page Date Name")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (page != "" && city != "" && (fromDateLong > 0 && toDateLong > 0) && name != "") {
      println("Page City Date Name")
      Json.obj(
        "page" -> Json.obj("$regex" -> ("^" + page.toLowerCase + ".*")),
        "city" -> city,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (city != "" && (fromDateLong > 0 && toDateLong > 0) && name != "" && page == "") {
      println("City Date Name")
      Json.obj(
        "city" -> city,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    }
  }

  override def listDetailAdvertiserIncome(page: String, city: String, fromDate: String, toDate: String, name: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val advertisers = advertisementCollection.flatMap(_.aggregate(
      UnwindField("advertise"),
      List(
        Lookup(
          "advertiser_tbl",
          "advertise.id",
          "_id",
          "advertiser"
        ),
        UnwindField("advertiser"),
        Project(
          Json.obj(
            "page" -> Json.obj("$toLower" -> "$page"),
            "location" -> 1,
            "discount" -> "$advertise.discount",
            "price" -> 1,
            "duration" -> "$advertise.duration",
            "startDate" -> "$advertise.startDate",
            "expireDate" -> "$advertise.expireDate",
            "id" -> "$advertiser._id",
            "name" -> Json.obj("$toLower" -> "$advertiser.name"),
            "company" -> "$advertiser.name",
            "description" -> "$advertiser.description",
            "phones" -> "$advertiser.phones",
            "email" -> "$advertiser.email",
            "city" -> "$advertiser.city",
            "address" -> "$advertiser.address",
            "image" -> "$advertiser.image",
            "url" -> "$advertiser.url",
            "createDate" -> "$advertiser.createDate",
            "status" -> "$advertiser.status"
          )
        ),
        Match(matchFilter(page, city, fromDate, toDate, name)),
        Group(JsString("$state"))(
          "advertisement" -> Push(
            Json.obj(
              "page" -> "$page",
              "location" -> "$location",
              "discount" -> "$discount",
              "price" -> "$price",
              "duration" -> "$duration",
              "startDate" -> "$startDate",
              "expireDate" -> "$expireDate",
              "id" -> "$id",
              "company" -> "$company",
              "description" -> "$description",
              "phones" -> "$phones",
              "email" -> "$email",
              "city" -> "$city",
              "address" -> "$address",
              "image" -> "$image",
              "url" -> "$url",
              "createDate" -> "$createDate",
              "status" -> "$status"
            )
          ),
          "total" -> SumAll
        ),
        UnwindField("advertisement"),
        Project(
          Json.obj(
            "page" -> "$advertisement.page",
            "location" -> "$advertisement.location",
            "discount" -> "$advertisement.discount",
            "price" -> "$advertisement.price",
            "duration" -> "$advertisement.duration",
            "startDate" -> "$advertisement.startDate",
            "expireDate" -> "$advertisement.expireDate",
            "id" -> "$advertisement.id",
            "company" -> "$advertisement.company",
            "description" -> "$advertisement.description",
            "phones" -> "$advertisement.phones",
            "email" -> "$advertisement.email",
            "city" -> "$advertisement.city",
            "address" -> "$advertisement.address",
            "image" -> "$advertisement.image",
            "url" -> "$advertisement.url",
            "createDate" -> "$advertisement.createDate",
            "status" -> "$advertisement.status",
            "total" -> "$total"
          )
        ),
        Skip((start * limit) - limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    advertisers.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all sponsor")
    }
    advertisers
  }

  /* helper functions */
  def matchFilterCategory(startDate: String, endDate: String, name: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(startDate).getMillis;
    val toDateLong = FormatDate.parseDate(endDate).getMillis;
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;

    if ((fromDateLong > 0 && toDateLong > 0) && name == "") {
      println("Date")
      Json.obj(
        "advertise.startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && name != "") {
      println("Date Name")
      Json.obj(
        "advertise.startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (name != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "advertise.startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      //"advertise.startDate" -> Json.obj("$lt" -> nowLong)
      )
    }
  }

  override def listCategoryIncome(startDate: String, endDate: String, name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val advertisers = categoryCollection.flatMap(_.aggregate(
      UnwindField("advertise"),
      List(
        Lookup(
          "advertiser_tbl",
          "advertise.id",
          "_id",
          "advertiser"
        ),
        UnwindField("advertiser"),
        Project(
          Json.obj(
            "name" -> 1,
            "price" -> 1,
            "advertise.price" -> 1,
            "advertiser.name" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$advertiser.name"),
            "advertiser.city" -> 1,
            "advertiser.phones" -> 1,
            "advertiser.email" -> 1,
            "advertise.startDate" -> 1,
            "advertise.expireDate" -> 1
          )
        ),
        Match(matchFilterCategory(startDate, endDate, name))
      )
    ).map(_.head[JsObject]))
    advertisers.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all sponsor")
    }
    advertisers
  }

}

