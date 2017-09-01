package repositories.promoted

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.PromoteUser
import org.joda.time.DateTime
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, JsString, Json }
import reactivemongo.api.{ Cursor, QueryOpts, ReadPreference }
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
 * Created by Ky Sona on 5/3/2017.
 */
@ImplementedBy(classOf[PromoteMemberRepo])
trait IPromoteMemberRepo {
  def checkMemberRequest(id: BSONObjectID): Future[Option[PromoteUser]]
  def insertMemberRequest(promote: PromoteUser): Future[WriteResult]
  def listMemberRequests(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]
  def promoteMemberById(id: String, userId: String, userType: String, startDate: String, endDate: String): Future[WriteResult]
  def deleteMemberRequest(id: String): Future[WriteResult]
  def listMerchantExpired(page: Int, limit: Int): Future[List[JsObject]]
  def updateExpiredMerchants(): Future[List[JsObject]]
  def listMemberRequestsExpired(page: Int, limit: Int): Future[List[JsObject]]
  def deleteMemberRequestExpired(): Future[List[JsObject]]
  def checkMerchantExpired(userId: String): Future[Option[PromoteUser]]
}

class PromoteMemberRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IPromoteMemberRepo {
  def userCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))
  def promoteCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promote_user_tbl"))
  def packageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_user_package_tbl"))

  /* helper function */
  override def checkMemberRequest(id: BSONObjectID): Future[Option[PromoteUser]] = {
    val requested = promoteCollection.flatMap(_.find(
      obj(
        "userId" -> id,
        "status" -> 0
      )
    ).one[PromoteUser])
    requested.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("member requested already exist")
    }
    requested
  }

  override def insertMemberRequest(promote: PromoteUser): Future[WriteResult] = {
    val writeRes = promoteCollection.flatMap(_.insert(promote))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new promoted user")
    }
    writeRes
  }

  /* helper functions */
  def matchFilterRequest(name: String, location: String, fromDate: String, toDate: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis
    val toDateLong = FormatDate.parseDate(toDate).getMillis

    if (name != "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> 0
      )
    } else if (name != "" && location != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name Location Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "status" -> 0
      )
    } else if (name != "" && (fromDateLong > 0 && toDateLong > 0) && location == "") {
      println("Name Date Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if (name != "" && (fromDateLong < 0 && toDateLong < 0) && location == "") {
      println("Name Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> 0
      )
    } else if (location != "" && name == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Location Status")
      Json.obj(
        "users.city" -> location,
        "status" -> 0
      )
    } else if (location != "" && (fromDateLong > 0 && toDateLong > 0) && name == "") {
      println("Location Date Status")
      Json.obj(
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if (location != "" && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("Location Status")
      Json.obj(
        "users.city" -> location,
        "status" -> 0
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && location == "" && name == "") {
      println("Date Status")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && location == "" && name == "") {
      println("Date Type Status")
      Json.obj(
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if ((fromDateLong < 0 && toDateLong < 0) && location == "" && name == "") {
      println("Type Status")
      Json.obj(
        "status" -> 0
      )
    } else if (name != "" && location != "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Name Location Date Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if (name != "" && location != "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Name Location Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "status" -> 0
      )
    } else if (location != "" && (fromDateLong > 0 && toDateLong > 0) && name == "") {
      println("Location Date Type Status")
      Json.obj(
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else if (name != "" && location != "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Name Location Date Status")
      Json.obj(
        "users.userName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "users.city" -> location,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "status" -> 0
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "status" -> 0
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
        Lookup(
          "promoted_user_package_tbl",
          "packageId",
          "_id",
          "packages"
        ),
        Project(
          Json.obj(
            "status" -> 1,
            "startDate" -> 1,
            "users.userName" -> 1,
            "users.city" -> 1,
            "packages.duration" -> 1,
            "packages.price" -> 1
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

  override def listMemberRequests(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountRequest(name, location, startDate, endDate), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("total : " + total)
    val requested = promoteCollection.flatMap(_.aggregate(
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
    requested.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get incomes")
    }
    requested
  }

  override def promoteMemberById(id: String, userId: String, userType: String, startDate: String, endDate: String): Future[WriteResult] = {
    val startdate = FormatDate.parseDate(startDate).getMillis
    val enddate = FormatDate.parseDate(endDate).getMillis
    // update status in promote_user_tbl
    val writeRes = promoteCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> 1,
          "startDate" -> startdate,
          "endDate" -> enddate
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => {
        println("Successful update status in promote_user_tbl")
        val writeRes = userCollection.flatMap(_.update(
          Json.obj("_id" -> BSONObjectID.parse(userId).get),
          BSONDocument(
            "$set" -> BSONDocument(
              "userType" -> userType,
              "status" -> 1
            )
          )
        ))
        writeRes.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(writeResult) => {
            println("Successful promote member to merchant")
          }
        }
        writeRes
      } // close case success
    }
    writeRes
  }

  override def deleteMemberRequest(id: String): Future[WriteResult] = {
    val writeRes = promoteCollection.flatMap(_.remove(obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully delete user request")
    }
    writeRes
  }

  /* helper function */
  def getCountRequest(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val total = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        Lookup(
          "promoted_user_package_tbl",
          "packageId",
          "_id",
          "packages"
        ),
        Project(
          Json.obj(
            "_id" -> 1,
            "status" -> 1,
            "endDate" -> 1,
            "users.status" -> 1,
            "packages.status" -> 1
          )
        ),
        Match(Json.obj(
          "status" -> 1,
          "users.status" -> 1,
          "packages.status" -> 1,
          "endDate" -> Json.obj("$lt" -> nowLong)
        )),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listMerchantExpired(page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val jsObj: List[JsObject] = Await.result(getCountRequest(), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("total : " + total)
    val users = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        Lookup(
          "promoted_user_package_tbl",
          "packageId",
          "_id",
          "packages"
        ),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users._id" -> 1,
            "users.status" -> 1,
            "users.userName" -> 1,
            "users.profileImage" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.otherPhones" -> 1,
            "users.city" -> 1,
            "packages._id" -> 1,
            "packages.status" -> 1,
            "packages.duration" -> 1,
            "packages.price" -> 1,
            "total" -> s"$total"
          )
        ),
        Match(Json.obj(
          "status" -> 1,
          "users.status" -> 1,
          "packages.status" -> 1,
          "endDate" -> Json.obj("$lt" -> nowLong)
        )),
        Skip((page * limit) - limit),
        Limit(limit),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users._id" -> 1,
            "users.userName" -> 1,
            "users.profileImage" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.otherPhones" -> 1,
            "users.city" -> 1,
            "packages._id" -> 1,
            "packages.duration" -> 1,
            "packages.price" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get incomes")
    }
    users
  }

  /*helper functions */
  def updateUserStatus(userId: BSONObjectID): Future[WriteResult] = {
    val writeRes = userCollection.flatMap(_.update(
      Json.obj("_id" -> userId),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> -1
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully updated user type to be normal")
    }
    writeRes
  }

  def updatePromoteUserStatus(id: BSONObjectID, userId: BSONObjectID): Future[WriteResult] = {
    val writeRes = promoteCollection.flatMap(_.update(
      Json.obj("_id" -> id),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> -1
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => {
        println("Successfully updated promote user status")
        updateUserStatus(userId)
      } //close case success
    }
    writeRes
  }

  override def updateExpiredMerchants(): Future[List[JsObject]] = {
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val merchants = promoteCollection.flatMap(_.find(Json.obj(
      "status" -> 1,
      "endDate" -> Json.obj("$lt" -> nowLong)
    ))
      .projection(
        Json.obj(
          "_id" -> 1,
          "userId" -> 1
        )
      )
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
    merchants.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => {
        println("Successfully get all expired promoted")
        for (d <- data) {
          val id = (d \ "_id").as[BSONObjectID]
          val userId = (d \ "userId").as[BSONObjectID]
          updatePromoteUserStatus(id, userId)
        }
      } // close case success
    }
    merchants
  }

  /* helper functions */
  def getCountMemberRequestsExpired(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val bonusDay = (1000 * 60 * 60 * 24) * 7;
    val deadLine = nowLong - bonusDay;
    val total = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        Lookup(
          "promoted_user_package_tbl",
          "packageId",
          "_id",
          "packages"
        ),
        Project(
          Json.obj(
            "_id" -> 1,
            "status" -> 1,
            "startDate" -> 1,
            "users.status" -> 1,
            "packages.status" -> 1
          )
        ),
        Match(Json.obj(
          "status" -> 0,
          "users.status" -> 1,
          "packages.status" -> 1,
          "startDate" -> Json.obj("$lt" -> deadLine)
        )),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful total")
    }
    total
  }

  override def listMemberRequestsExpired(page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val bonusDay = (1000 * 60 * 60 * 24) * 7;
    val deadLine = nowLong - bonusDay;
    val jsObj: List[JsObject] = Await.result(getCountMemberRequestsExpired(), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("total : " + total)
    val users = promoteCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "users"
      ),
      List(
        Lookup(
          "promoted_user_package_tbl",
          "packageId",
          "_id",
          "packages"
        ),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users._id" -> 1,
            "users.status" -> 1,
            "users.userName" -> 1,
            "users.profileImage" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.otherPhones" -> 1,
            "users.city" -> 1,
            "packages._id" -> 1,
            "packages.status" -> 1,
            "packages.duration" -> 1,
            "packages.price" -> 1,
            "total" -> s"$total"
          )
        ),
        Match(Json.obj(
          "status" -> 0,
          "users.status" -> 1,
          "packages.status" -> 1,
          "startDate" -> Json.obj("$lt" -> deadLine)
        )),
        Skip((page * limit) - limit),
        Limit(limit),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "users._id" -> 1,
            "users.userName" -> 1,
            "users.profileImage" -> 1,
            "users.phone" -> 1,
            "users.email" -> 1,
            "users.otherPhones" -> 1,
            "users.city" -> 1,
            "packages._id" -> 1,
            "packages.duration" -> 1,
            "packages.price" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get member requests expired")
    }
    users
  }

  /* helper function */
  def deleteMemberRequest(id: BSONObjectID): Future[WriteResult] = {
    val writeRes = promoteCollection.flatMap(_.remove(obj(
      "_id" -> id
    )))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully delete user request expired")
    }
    writeRes
  }

  override def deleteMemberRequestExpired(): Future[List[JsObject]] = {
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val bonusDay = (1000 * 60 * 60 * 24) * 7;
    val deadLine = nowLong - bonusDay;
    val users = promoteCollection.flatMap(_.find(Json.obj(
      "status" -> 0,
      "startDate" -> Json.obj("$lt" -> deadLine)
    ))
      .projection(
        Json.obj(
          "_id" -> 1
        )
      )
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => {
        println("Successfully get all expired user requested.")
        for (d <- data) {
          val id = (d \ "_id").as[BSONObjectID]
          deleteMemberRequest(id)
        }
      } // close case success
    }
    users
  }

  override def checkMerchantExpired(userId: String): Future[Option[PromoteUser]] = {
    val merchant = promoteCollection.flatMap(_.find(
      obj(
        "userId" -> BSONObjectID.parse(userId).get,
        "status" -> 1
      )
    ).one[PromoteUser])
    merchant.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("success get merchant")
    }
    merchant
  }

}
