package repositories.advertiser

import com.google.inject.{ ImplementedBy, Inject }
import models.advertiser.{ Advertiser, Advertising }
import play.api.libs.json.{ JsObject, JsString, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import utils.FormatDate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Random, Success }

@ImplementedBy(classOf[AdvertiserRepo])
trait IAdvertiserRepo {

  def insertAdvertiser(advertiser: Advertiser): Future[WriteResult]

  def insertAdvertising(advertising: Advertising): Future[WriteResult]

  def updateAdvertiser(id: String, advertiser: Advertiser): Future[WriteResult]

  def updateAdvertising(id: String, advertising: Advertising): Future[WriteResult]

  def deleteAdvertiser(id: String, status: Int): Future[WriteResult]

  def getFilterAdvertisers(name: String, city: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]]

  def getAdvertiserById(id: String): Future[Option[JsObject]]

  def getAdvertisingByPageLocation(page: String, location: String, price: Double, dateStart: String, dateEnd: String): Future[Option[JsObject]]

  def updateAdvertiserId(id: String, advertiserId: List[BSONObjectID]): Future[WriteResult]
}

class AdvertiserRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IAdvertiserRepo {

  def advertiserColl = reactiveMongoApi.database.map(_.collection[JSONCollection]("advertiser_tbl"))

  def advertisingColl = reactiveMongoApi.database.map(_.collection[JSONCollection]("advertising_tbl"))

  def matchFilter(name: String, city: String, status: Int, fromDate: String, toDate: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis
    val toDateLong = FormatDate.parseDate(toDate).getMillis
    println(fromDateLong, toDateLong)
    if (name != "" && city == "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("N")
      Json.obj("lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")))
    } else if (name == "" && city != "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("L")
      Json.obj(
        "city" -> city
      )
    } else if ((status > -2 && status < 2) && name == "" && city == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("S")
      Json.obj(
        "status" -> status
      )
    } else if (name == "" && city == "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("D")
      Json.obj(
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && city != "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NL")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "city" -> city
      )
    } else if (name != "" && city == "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NS")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> status
      )
    } else if (name != "" && city == "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("ND")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && city != "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NLS")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "city" -> city,
        "status" -> status
      )
    } else if (name != "" && city != "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NLD")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "city" -> city,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && city == "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NSD")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && city != "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("LS")
      Json.obj(
        "city" -> city,
        "status" -> status
      )
    } else if (name == "" && city != "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("LD")
      Json.obj(
        "city" -> city,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && city != "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("LSD")
      Json.obj(
        "city" -> city,
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && city == "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("SD")
      Json.obj(
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && city != "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NLSD")
      Json.obj(
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "city" -> city,
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "userType" -> "advertiser"
      )
    }
  }

  def getCountAllAdvertisers(name: String, city: String, status: Int, fromDate: String, toDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val users = advertiserColl.flatMap(_.aggregate(
      Lookup(
        "advertising_tbl",
        "_id",
        "advertiserId",
        "advertising"
      ),
      List(
        UnwindField("advertising"),
        Project(
          Json.obj(
            "name" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$name"),
            "description" -> 1,
            "phones" -> 1,
            "email" -> 1,
            "city" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "dateJoin" -> 1,
            "deleted" -> 1,
            "adsPage" -> "$advertising.page",
            "adsLocation" -> "$advertising.location",
            "adsDateStart" -> "$advertising.dateStart",
            "adsDateEnd" -> "$advertising.dateEnd"
          )
        ),
        Match(matchFilter(name, city, status, fromDate, toDate)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get all sponsor with result : $data")
    }
    users
  }

  override def insertAdvertiser(advertiser: Advertiser): Future[WriteResult] = {
    val writeRes = advertiserColl.flatMap(_.insert(advertiser))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new advertiser with result $writeResult")
    }
    writeRes
  }

  override def insertAdvertising(advertising: Advertising): Future[WriteResult] = {
    val writeRes = advertisingColl.flatMap(_.insert(advertising))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new advertiser with result $writeResult")
    }
    writeRes
  }

  override def updateAdvertiser(id: String, advertiser: Advertiser): Future[WriteResult] = {
    val writeRes = advertiserColl.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "name" -> advertiser.name,
          "description" -> advertiser.description,
          "phones" -> advertiser.phones,
          "email" -> advertiser.email,
          "city" -> advertiser.city,
          "address" -> advertiser.address
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update advertiser with result $data")
    }
    writeRes
  }

  override def updateAdvertising(id: String, advertising: Advertising): Future[WriteResult] = {
    val writeRes = advertisingColl.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "name" -> advertising.ads_name,
          "description" -> advertising.description,
          "page" -> advertising.page,
          "location" -> advertising.location,
          "price" -> advertising.price,
          "dateStart" -> advertising.dateStart.get.getMillis,
          "dateEnd" -> advertising.dateEnd.get.getMillis
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update advertising with result $data")
    }
    writeRes
  }

  override def deleteAdvertiser(id: String, status: Int): Future[WriteResult] = {
    val writeRes = advertiserColl.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> status
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful delete advertiser with result $data")
    }
    writeRes
  }

  override def getFilterAdvertisers(name: String, city: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountAllAdvertisers(name, city, status, fromDate, toDate), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    val users = advertiserColl.flatMap(_.aggregate(
      Lookup(
        "advertising_tbl",
        "_id",
        "advertiserId",
        "advertising"
      ),
      List(
        UnwindField("advertising"),
        Project(
          Json.obj(
            "name" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$name"),
            "description" -> 1,
            "phones" -> 1,
            "email" -> 1,
            "city" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "dateJoin" -> 1,
            "status" -> 1,
            "adsId" -> "$advertising._id",
            "adsName" -> "$advertising.name",
            "adsDescription" -> "$advertising.description",
            "adsPage" -> "$advertising.page",
            "adsLocation" -> "$advertising.location",
            "adsPrice" -> "$advertising.price",
            "adsDiscount" -> "$advertising.discount",
            "adsDateStart" -> "$advertising.dateStart",
            "adsDateEnd" -> "$advertising.dateEnd"
          )
        ),
        Match(matchFilter(name, city, status, fromDate, toDate)),
        Sort(Ascending("lowName")),
        Skip((start - 1) * limit),
        Limit(limit),
        Project(
          Json.obj(
            "name" -> 1,
            "description" -> 1,
            "phones" -> 1,
            "email" -> 1,
            "city" -> 1,
            "address" -> 1,
            "status" -> 1,
            "adsId" -> 1,
            "adsName" -> 1,
            "adsDescription" -> 1,
            "adsPage" -> 1,
            "adsLocation" -> 1,
            "adsPrice" -> 1,
            "adsDiscount" -> 1,
            "adsDateStart" -> 1,
            "adsDateEnd" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get all sponsor with result : $data")
    }
    users
  }

  override def getAdvertiserById(id: String): Future[Option[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val users = advertiserColl.flatMap(_.aggregate(
      Lookup(
        "advertising_tbl",
        "_id",
        "advertiserId",
        "advertising"
      ),
      List(
        UnwindField("advertising"),
        Match(Json.obj("_id" -> BSONObjectID.parse(id).get))
      )
    ).map(_.head[JsObject].headOption))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertiser by id with result : $data")
    }
    users
  }

  override def getAdvertisingByPageLocation(page: String, location: String, price: Double, dateStart: String, dateEnd: String): Future[Option[JsObject]] = {
    val dateStartLong = FormatDate.parseDate(dateStart).getMillis
    val dateEndLong = FormatDate.parseDate(dateEnd).getMillis

    val advertising = advertisingColl.flatMap(_.find(
      Json.obj(
        "page" -> page,
        "location" -> location,
        "price" -> price,
        "dateStart" -> dateStartLong,
        "dateEnd" -> dateEndLong
      )
    ).one[JsObject])
    advertising.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertising by page and location with result $data")
    }
    advertising
  }

  override def updateAdvertiserId(id: String, advertiserId: List[BSONObjectID]): Future[WriteResult] = {
    val writeRes = advertisingColl.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "advertiserId" -> advertiserId
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update advertising with result $data")
    }
    writeRes
  }
}