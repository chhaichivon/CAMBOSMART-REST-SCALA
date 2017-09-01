package repositories.advertisement

import com.google.inject.{ ImplementedBy, Inject }
import models.advertisement.{ Advertise, CategoryAdvertisement }
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
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[CategoryAdvertisementRepo])
trait ICategoryAdvertisementRepo {

  def insertCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult]

  def updateCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult]

  def deleteCategoryAdvertisement(id: String): Future[WriteResult]

  def getCategoryAdvertisementById(id: String): Future[Option[CategoryAdvertisement]]

  def getCategoryAdvertisementByCategoryId(id: BSONObjectID): Future[Option[CategoryAdvertisement]]

  def getCategoryAdvertisements: Future[List[CategoryAdvertisement]]

  def getCategoryAdvertisementSchedule(id: String): Future[List[JsObject]]

  def updateCategoryAdvertisementInPackage(id: String, advertise: Advertise): Future[WriteResult]

  def getCategoryAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]

  def getCategoryAdvertiser(id: String, startDate: Long, expireDate: Long): Future[List[JsObject]]

  def updateAdvertise(advertiserId: String): Future[WriteResult]

  def getDisplayCategoryAdvertisements: Future[List[JsObject]]
}

class CategoryAdvertisementRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends ICategoryAdvertisementRepo {

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("category_advertisement_tbl"))

  override def insertCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(categoryAdvertisement))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new category advertisement with result $writeResult")
    }
    writeRes
  }

  override def updateCategoryAdvertisement(categoryAdvertisement: CategoryAdvertisement): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> categoryAdvertisement._id.get),
      BSONDocument(
        "$set" -> BSONDocument(
          "name" -> categoryAdvertisement.name,
          "description" -> categoryAdvertisement.description,
          "price" -> categoryAdvertisement.price
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update category advertisement package")
    }
    writeRes
  }

  override def deleteCategoryAdvertisement(id: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.remove(Json.obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful delete category advertisement with result $data")
    }
    writeRes
  }

  override def getCategoryAdvertisementById(id: String): Future[Option[CategoryAdvertisement]] = {
    val advertisement = collection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get)).one[CategoryAdvertisement])
    advertisement.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get category advertisement by id with result $data")
    }
    advertisement
  }

  override def getCategoryAdvertisementByCategoryId(id: BSONObjectID): Future[Option[CategoryAdvertisement]] = {
    val advertisement = collection.flatMap(_.find(Json.obj("categoryId" -> id)).one[CategoryAdvertisement])
    advertisement.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get category advertisement by id with result $data")
    }
    advertisement
  }

  override def getCategoryAdvertisements: Future[List[CategoryAdvertisement]] = {
    val advertisements = collection.flatMap(_.find(Json.obj())
      .cursor[CategoryAdvertisement](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[CategoryAdvertisement]]()))
    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get category advertisement by id with result $data")
    }
    advertisements
  }

  override def getCategoryAdvertisementSchedule(id: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (id != "") {
        Json.obj("categoryId" -> BSONObjectID.parse(id).get, "advertise.expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis))
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
        Sort(Ascending("name")),
        Sort(Ascending("advertise.expireDate")),
        Group(JsString("$state"))(
          "advertisement" -> Push(
            Json.obj(
              "company" -> "$advertiser.name",
              "phones" -> "$advertiser.phones",
              "name" -> "$name",
              "uPrice" -> "$price",
              "startDate" -> "$advertise.startDate",
              "expireDate" -> "$advertise.expireDate",
              "oPrice" -> "$advertise.price"
            )
          ),
          "total" -> SumAll
        ),
        UnwindField("advertisement"),
        Project(
          Json.obj(
            "_id" -> 0,
            "company" -> "$advertisement.company",
            "phones" -> "$advertisement.phones",
            "name" -> "$advertisement.name",
            "uPrice" -> "$advertisement.uPrice",
            "startDate" -> "$advertisement.startDate",
            "expireDate" -> "$advertisement.expireDate",
            "oPrice" -> "$advertisement.oPrice",
            "total" -> "$total"
          )
        )
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get schedule category advertisements !!")
    }
    advertisements
  }

  override def updateCategoryAdvertisementInPackage(id: String, advertise: Advertise): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("categoryId" -> BSONObjectID.parse(id).get),
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
      case Success(data) => println("Successful update advertise in category advertisement package")
    }
    writeRes
  }

  override def getCategoryAdvertisers(name: String, location: String, status: Int, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._

    val startLong = FormatDate.parseDate(startDate).getMillis
    val endLong = FormatDate.parseDate(endDate).getMillis
    val nowLong = new DateTime().getMillis

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
        Json.obj()
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
            "categoryId" -> 1,
            "advertisementName" -> "$name",
            "uPrice" -> "$price",
            "oPrice" -> "$advertise.price",
            "discount" -> "$advertise.discount",
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
              "categoryId" -> "$categoryId",
              "name" -> "$advertisementName",
              "uPrice" -> "$uPrice",
              "oPrice" -> "$oPrice",
              "discount" -> "$discount",
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
            "categoryId" -> "$advertisement.categoryId",
            "name" -> "$advertisement.name",
            "discount" -> "$advertisement.discount",
            "uPrice" -> "$advertisement.uPrice",
            "oPrice" -> "$advertisement.oPrice",
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
        Skip((page - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get category advertisements and advertisers !!")
    }
    advertisements
  }

  override def getCategoryAdvertiser(id: String, startDate: Long, expireDate: Long): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val advertiser = collection.flatMap(_.aggregate(
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
            "categoryId" -> 1,
            "name" -> 1,
            "descriptionPackage" -> "$description",
            "uPrice" -> "$price",
            "oPrice" -> "$advertise.price",
            "discount" -> "$advertise.discount",
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
        Match(Json.obj(
          "id" -> BSONObjectID.parse(id).get,
          "startDate" -> startDate,
          "expireDate" -> expireDate
        ))
      )
    ).map(_.head[JsObject]))

    advertiser.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get category advertisement and advertiser !!")
    }
    advertiser
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

  override def getDisplayCategoryAdvertisements: Future[List[JsObject]] = {
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
        Project(Json.obj(
          "categoryId" -> 1,
          "name" -> 1,
          "startDate" -> "$advertise.startDate",
          "expireDate" -> "$advertise.expireDate",
          "company" -> "$advertiser.name",
          "url" -> "$advertiser.url",
          "image" -> "$advertiser.image"
        )),
        Match(Json.obj(
          "startDate" -> Json.obj("$lte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis),
          "expireDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis)
        )),
        Project(Json.obj(
          "categoryId" -> 1,
          "name" -> 1,
          "startDate" -> 1,
          "expireDate" -> 1,
          "company" -> 1,
          "url" -> 1,
          "image" -> 1
        ))
      )
    ).map(_.head[JsObject]))
    advertisements.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get display category advertisement !!")
    }
    advertisements
  }
}

