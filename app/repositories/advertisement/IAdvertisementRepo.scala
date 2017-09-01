package repositories.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.{ Advertise, Advertisement }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsString, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import utils.FormatDate

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[AdvertisementRepo])
trait IAdvertisementRepo {
  def insertAdvertisement(advertisement: Advertisement): Future[WriteResult]

  def updateAdvertisement(advertisement: Advertisement): Future[WriteResult]

  def updateAdvertiseInAdvertisement(page: String, location: String, advertise: Advertise): Future[WriteResult]

  def updateAdvertise(advertiserId: String): Future[WriteResult]

  def deleteAdvertisement(advertisementId: String): Future[WriteResult]

  def getAdvertisementById(advertisementId: String): Future[Option[Advertisement]]

  def validatePageAndLocation(page: String): Future[List[Advertisement]]

  def getAdvertisementByPageAndLocation(page: String, location: String): Future[List[JsObject]]

  def getAdvertisementAndAdvertiserByPageAndLocation(page: String, location: String, start: Int, limit: Int): Future[List[JsObject]]

  def getAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, start: Int, limit: Int): Future[List[JsObject]]

  def getAdvertiserById(advertiserId: BSONObjectID, location: String, startDate: Long, expireDate: Long): Future[List[JsObject]]

  def getDisplayAdvertisements: Future[List[JsObject]]
}

class AdvertisementRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends IAdvertisementRepo {

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("advertisement_tbl"))

  override def insertAdvertisement(advertisement: Advertisement): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(advertisement))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new advertisement with result $writeResult")
    }
    writeRes
  }

  override def updateAdvertisement(advertisement: Advertisement): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> advertisement._id.get),
      BSONDocument(
        "$set" -> BSONDocument(
          "description" -> advertisement.description,
          "price" -> advertisement.price
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update advertisement package")
    }
    writeRes
  }

  override def updateAdvertiseInAdvertisement(page: String, location: String, advertise: Advertise): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("page" -> page, "location" -> location),
      BSONDocument(
        "$push" -> BSONDocument(
          "advertise" -> BSONDocument(
            "id" -> advertise.id.get,
            "duration" -> advertise.duration,
            "startDate" -> FormatDate.parseDate(FormatDate.printDate(advertise.startDate)).getMillis,
            "expireDate" -> FormatDate.parseDate(FormatDate.printDate(advertise.expireDate)).getMillis,
            "price" -> advertise.price,
            "discount" -> advertise.discount
          )
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful update advertise in advertisement package")
    }
    writeRes
  }

  override def updateAdvertise(advertiserId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj(
        "advertise" -> Json.obj("$elemMatch" -> Json.obj("id" -> BSONObjectID.parse(advertiserId).get, "expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis)))
      ),
      BSONDocument(
        "$set" -> BSONDocument(
          "advertise.$.expireDate" -> FormatDate.parseDate(FormatDate.printDate(new DateTime() minusDays 1)).getMillis
        )
      ),
      multi = true
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update advertise to expire with result $data")
    }
    writeRes
  }

  override def deleteAdvertisement(advertisementId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.remove(Json.obj("_id" -> BSONObjectID.parse(advertisementId).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful delete advertisement with result $data")
    }
    writeRes

  }

  override def getAdvertisementById(advertisementId: String): Future[Option[Advertisement]] = {
    val advertisement = collection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(advertisementId).get)).one[Advertisement])
    advertisement.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertisement by id with result $data")
    }
    advertisement
  }

  override def validatePageAndLocation(page: String): Future[List[Advertisement]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (page != "") {
        Json.obj("lowerPage" -> page.toLowerCase)
      } else {
        Json.obj()
      }
    }
    val advertisements = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "page" -> 1,
          "lowerPage" -> Json.obj("$toLower" -> "$page"),
          "location" -> 1,
          "description" -> 1,
          "price" -> 1,
          "advertise" -> 1
        )
      ),
      List(
        Match(filter)
      )
    ).map(_.head[Advertisement]))

    /*val advertisements = collection.flatMap(_.find(filter)
      .cursor[Advertisement](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Advertisement]]()))*/
    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get advertisements !!")
    }
    advertisements
  }

  override def getAdvertisementByPageAndLocation(page: String, location: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (page != "" && location == "") {
        Json.obj("lowerPage" -> page.toLowerCase, "advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
      } else if (page != "" && location != "") {
        Json.obj("lowerPage" -> page.toLowerCase, "location" -> location, "advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
      } else {
        Json.obj("advertise.expireDate" -> Json.obj("$gte" -> new DateTime().getMillis))
      }
    }
    val advertisements = collection.flatMap(_.aggregate(
      UnwindField("advertise"),
      List(
        Project(
          Json.obj(
            "page" -> 1,
            "lowerPage" -> Json.obj("$toLower" -> "$page"),
            "location" -> 1,
            "description" -> 1,
            "price" -> 1,
            "advertise" -> 1
          )
        ),
        Match(filter),
        Sort(Ascending("advertise.expireDate")),
        Group(Json.obj("_id" -> "$_id", "page" -> "$page", "location" -> "$location", "description" -> "$description", "price" -> "$price"))(
          "advertise" -> PushField("advertise")
        ),
        Project(
          Json.obj(
            "_id" -> "$_id._id",
            "page" -> "$_id.page",
            "location" -> "$_id.location",
            "description" -> "$_id.description",
            "price" -> "$_id.price",
            "advertise" -> "$advertise"
          )
        )
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertisements with result: $data")
    }
    advertisements
  }

  override def getAdvertisementAndAdvertiserByPageAndLocation(page: String, location: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (page != "" && location == "") {
        Json.obj("page" -> page, "advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
      } else if (page != "" && location != "") {
        Json.obj("page" -> page, "location" -> location, "advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
      } else {
        Json.obj("advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
      }
    }
    val advertisements = collection.flatMap(_.aggregate(
      UnwindField("advertise"),
      List(
        Lookup(
          "advertiser_tbl",
          "advertise.id",
          "_id",
          "advertiser"
        ),
        UnwindField("advertiser"),
        Match(filter),
        Sort(Ascending("page")),
        Sort(Ascending("advertise.expireDate")),
        Group(JsString("$state"))(
          "advertisement" -> Push(
            Json.obj(
              "name" -> "$advertiser.name",
              "phones" -> "$advertiser.phones",
              "page" -> "$page",
              "location" -> "$location",
              "price" -> "$price",
              "startDate" -> "$advertise.startDate",
              "expireDate" -> "$advertise.expireDate"
            )
          ),
          "total" -> SumAll
        ),
        UnwindField("advertisement"),
        Project(
          Json.obj(
            "_id" -> 0,
            "name" -> "$advertisement.name",
            "phones" -> "$advertisement.phones",
            "page" -> "$advertisement.page",
            "location" -> "$advertisement.location",
            "price" -> "$advertisement.price",
            "startDate" -> "$advertisement.startDate",
            "expireDate" -> "$advertisement.expireDate",
            "total" -> "$total"
          )
        ),
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertisements and advertisers with result: $data")
    }
    advertisements
  }

  override def getAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._

    val startLong = FormatDate.parseDate(startDate).getMillis
    val endLong = FormatDate.parseDate(endDate).getMillis
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis

    def filter: JsObject = {
      if (name != "" && location == "" && status == 0 && (startLong < 0 && endLong < 0)) {
        println("N")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
        )
      } else if (name != "" && location != "" && status == 0 && (startLong < 0 && endLong < 0)) {
        println("N & L")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location
        )
      } else if (name != "" && location != "" && status == 1 && (startLong < 0 && endLong < 0)) {
        println("N & L & S1")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location,
          "expireDate" -> Json.obj("$gte" -> nowLong)
        )
      } else if (name != "" && location != "" && status == -1 && (startLong < 0 && endLong < 0)) {
        println("N & L & S-1")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location,
          "expireDate" -> Json.obj("$lt" -> nowLong)
        )
      } else if (name != "" && location != "" && status == 1 && (startLong > 0 && endLong > 0)) {
        println("N & L & S1 & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location,
          "expireDate" -> Json.obj("$gte" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name != "" && location != "" && status == -1 && (startLong > 0 && endLong > 0)) {
        println("N & L & S-1 & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location,
          "expireDate" -> Json.obj("$lt" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name != "" && location != "" && status == 0 && (startLong > 0 && endLong > 0)) {
        println("N & L & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "city" -> location,
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name != "" && location == "" && status == 1 && (startLong < 0 && endLong < 0)) {
        println("N & S1")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "expireDate" -> Json.obj("$gte" -> nowLong)
        )
      } else if (name != "" && location == "" && status == -1 && (startLong < 0 && endLong < 0)) {
        println("N & S-1")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "expireDate" -> Json.obj("$lt" -> nowLong)
        )
      } else if (name != "" && location == "" && status == 1 && (startLong > 0 && endLong > 0)) {
        println("N & S1 & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "expireDate" -> Json.obj("$gte" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name != "" && location == "" && status == -1 && (startLong > 0 && endLong > 0)) {
        println("N & S-1 & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "expireDate" -> Json.obj("$lt" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name != "" && location == "" && status == 0 && (startLong > 0 && endLong > 0)) {
        println("N & D")
        Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location != "" && status == 0 && (startLong < 0 && endLong < 0)) {
        println("L")
        Json.obj(
          "city" -> location
        )
      } else if (name == "" && location != "" && status == 1 && (startLong < 0 && endLong < 0)) {
        println("L & S1")
        Json.obj(
          "city" -> location,
          "expireDate" -> Json.obj("$gte" -> nowLong)
        )
      } else if (name == "" && location != "" && status == -1 && (startLong < 0 && endLong < 0)) {
        println("L & S-1")
        Json.obj(
          "city" -> location,
          "expireDate" -> Json.obj("$lt" -> nowLong)
        )
      } else if (name == "" && location != "" && status == 1 && (startLong > 0 && endLong > 0)) {
        println("L & S1 & D")
        Json.obj(
          "city" -> location,
          "expireDate" -> Json.obj("$gte" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location != "" && status == -1 && (startLong > 0 && endLong > 0)) {
        println("L & S-1 & D")
        Json.obj(
          "city" -> location,
          "expireDate" -> Json.obj("$lt" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location != "" && status == 0 && (startLong > 0 && endLong > 0)) {
        println("L & D")
        Json.obj(
          "city" -> location,
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location == "" && status == 1 && (startLong < 0 && endLong < 0)) {
        println("S1")
        Json.obj(
          "expireDate" -> Json.obj("$gte" -> nowLong)
        )
      } else if (name == "" && location == "" && status == -1 && (startLong < 0 && endLong < 0)) {
        println("S-1")
        Json.obj(
          "expireDate" -> Json.obj("$lt" -> nowLong)
        )
      } else if (name == "" && location == "" && status == 1 && (startLong > 0 && endLong > 0)) {
        println("S1 & D")
        Json.obj(
          "expireDate" -> Json.obj("$gte" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location == "" && status == -1 && (startLong > 0 && endLong > 0)) {
        println("S-1 & D")
        Json.obj(
          "expireDate" -> Json.obj("$lt" -> nowLong),
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else if (name == "" && location == "" && status == 0 && (startLong > 0 && endLong > 0)) {
        println("D")
        Json.obj(
          "createDate" -> Json.obj("$gte" -> startLong, "$lte" -> endLong)
        )
      } else {
        println("Default")
        Json.obj("expireDate" -> Json.obj("$gte" -> nowLong))
      }
    }

    val advertisements = collection.flatMap(_.aggregate(
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
            "page" -> 1,
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
        Match(filter),
        Sort(Ascending("page")),
        Sort(Ascending("name")),
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
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get advertisements and advertisers !!")
    }
    advertisements
  }

  override def getAdvertiserById(advertiserId: BSONObjectID, location: String, startDate: Long, expireDate: Long): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val advertisements = collection.flatMap(_.aggregate(
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
            "page" -> 1,
            "location" -> 1,
            "discount" -> "$advertise.discount",
            "price" -> 1,
            "duration" -> "$advertise.duration",
            "startDate" -> "$advertise.startDate",
            "expireDate" -> "$advertise.expireDate",
            "id" -> "$advertiser._id",
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
        Match(
          Json.obj(
            "id" -> advertiserId,
            "location" -> location,
            "startDate" -> startDate,
            "expireDate" -> expireDate
          )
        )
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get advertisement and advertiser with result: $data")
    }
    advertisements
  }

  override def getDisplayAdvertisements: Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val advertisements = collection.flatMap(_.aggregate(
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
            "page" -> 1,
            "location" -> 1,
            "price" -> 1,
            "advertiser" -> Json.obj(
              "id" -> "$advertiser._id",
              "name" -> "$advertiser.name",
              "description" -> "$advertiser.description",
              "phones" -> "$advertiser.phones",
              "email" -> "$advertiser.email",
              "city" -> "$advertiser.city",
              "address" -> "$advertiser.address",
              "image" -> "$advertiser.image",
              "url" -> "$advertiser.url",
              "createDate" -> "$advertiser.createDate",
              "status" -> "$advertiser.status",
              "duration" -> "$advertise.duration",
              "startDate" -> "$advertise.startDate",
              "expireDate" -> "$advertise.expireDate",
              "discount" -> "$advertise.discount"
            )
          )
        ),
        Match(
          Json.obj(
            "advertiser.startDate" -> Json.obj("$lte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis),
            "advertiser.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis)
          )
        ),
        Sample(Int.MaxValue),
        Group(Json.obj("page" -> "$page", "location" -> "$location", "price" -> "$price"))(
          "advertisers" -> PushField("advertiser")
        ),
        Project(
          Json.obj(
            "_id" -> 0,
            "page" -> "$_id.page",
            "location" -> "$_id.location",
            "price" -> "$_id.price",
            "advertisers" -> "$advertisers"
          )
        )
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get display advertisements !!")
    }
    advertisements
  }
}