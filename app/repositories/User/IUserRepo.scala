package repositories.User

import com.google.inject.{ ImplementedBy, Inject }
import models.promoted.{ PromoteUser, PromoteUserPackage }
import models.users.User
import org.joda.time.DateTime
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, JsString, Json }
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import utils.FormatDate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[UserRepo])
trait IUserRepo {

  def getFilterMembers(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]]

  /* sona */
  def getMemberById(id: String): Future[Option[User]]
  def blockMemberProducts(ids: List[BSONObjectID], status: Int): Future[WriteResult]
  def blockMemberById(id: String, status: Int): Future[WriteResult]

  /* naseat */
  def blockMerchantById(id: String): Future[WriteResult]
  def getMerchantById(id: String): Future[Option[User]]

  /*chivon*/
  def getFilterMemberRepo(name: String, location: String, status: Int, fromDate: DateTime, toDate: DateTime, start: Int, limit: Int): Future[List[JsObject]]

  def getMember(name: String, limit: Int, location: String, status: Int): Future[List[JsObject]]

  def updateMember(id: BSONObjectID, user: User): Future[Boolean]

  def deleteMember(id: BSONObjectID): Future[Boolean]

  def getUserWithStoreInfoRepo(username: String): Future[List[JsObject]]

  /*upload image */
  def uploadImage(id: String, image: String): Future[WriteResult]

  /*update profile*/
  def updateMemberProfile(id: String, userName: String, phone: String, email: String, phones: List[String], location: String, address: String): Future[WriteResult]

  def changePassword(id: String, newPassword: String): Future[WriteResult]

  def countAllNormalMembers(): Future[Int]
  def countAllMerchantMembers(): Future[Int]
}

class UserRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IUserRepo {

  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))
  def storeCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("store_tbl"))
  def productCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("product_tbl"))
  def promoteUserCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promote_user_tbl"))
  def promotePackageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_user_package_tbl"))

  def matchFilter(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(fromDate).getMillis
    val toDateLong = FormatDate.parseDate(toDate).getMillis
    println(fromDateLong, toDateLong)
    if (name != "" && location == "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("N")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if (name == "" && location != "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("L")
      Json.obj(
        "userType" -> userType,
        "location" -> location
      )
    } else if ((status > -2 && status < 2) && name == "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("S")
      Json.obj(
        "userType" -> userType,
        "status" -> status
      )
    } else if (name == "" && location == "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("D")
      Json.obj(
        "userType" -> userType,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && location != "" && (status < -1 || status > 1) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NL")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "location" -> location
      )
    } else if (name != "" && location == "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NS")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> status
      )
    } else if (name != "" && location == "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("ND")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && location != "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("NLS")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "location" -> location,
        "status" -> status
      )
    } else if (name != "" && location != "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NLD")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "location" -> location,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && location == "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NSD")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && location != "" && (status > -2 && status < 2) && (fromDateLong < 0 && toDateLong < 0)) {
      println("LS")
      Json.obj(
        "userType" -> userType,
        "location" -> location,
        "status" -> status
      )
    } else if (name == "" && location != "" && (status < -1 || status > 1) && (fromDateLong > 0 && toDateLong > 0)) {
      println("LD")
      Json.obj(
        "userType" -> userType,
        "location" -> location,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && location != "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("LSD")
      Json.obj(
        "userType" -> userType,
        "location" -> location,
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name == "" && location == "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("SD")
      Json.obj(
        "userType" -> userType,
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (name != "" && location != "" && (status > -2 && status < 2) && (fromDateLong > 0 && toDateLong > 0)) {
      println("NLSD")
      Json.obj(
        "userType" -> userType,
        "name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
        "location" -> location,
        "status" -> status,
        "dateJoin" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "userType" -> userType
      )
    }
  }

  def getCountFilterMembers(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "mistakeId" -> 1,
          "socialId" -> 1,
          "userName" -> 1,
          "name" -> Json.obj("$toLower" -> "$userName"),
          "phone" -> 1,
          "email" -> 1,
          "password" -> 1,
          "verifiedCode" -> 1,
          "otherPhones" -> 1,
          "location" -> "$city",
          "address" -> 1,
          "userType" -> 1,
          "dateJoin" -> 1,
          "dateBlock" -> 1,
          "profileImage" -> 1,
          "status" -> 1,
          "online" -> 1
        )
      ),
      List(
        Match(matchFilter(userType, name, location, status, fromDate, toDate)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get count ${userType.concat("s")} with result : $data")
    }
    total
  }

  override def getFilterMembers(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountFilterMembers(userType, name, location, status, fromDate, toDate), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    val users = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "mistakeId" -> 1,
          "socialId" -> 1,
          "userName" -> 1,
          "name" -> Json.obj("$toLower" -> "$userName"),
          "phone" -> 1,
          "email" -> 1,
          "otherPhones" -> 1,
          "location" -> "$city",
          "address" -> 1,
          "userType" -> 1,
          "dateJoin" -> 1,
          "dateBlock" -> 1,
          "profileImage" -> 1,
          "status" -> 1,
          "online" -> 1
        )
      ),
      List(
        Match(matchFilter(userType, name, location, status, fromDate, toDate)),
        Sort(Ascending("_id")),
        Skip((start - 1) * limit),
        Limit(limit),
        Project(
          Json.obj(
            "_id" -> 1,
            "mistakeId" -> 1,
            "socialId" -> 1,
            "userName" -> 1,
            "phone" -> 1,
            "email" -> 1,
            "otherPhones" -> 1,
            "location" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "dateJoin" -> 1,
            "dateBlock" -> 1,
            "profileImage" -> 1,
            "status" -> 1,
            "online" -> 1,
            "total" -> "$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(user) => println("Successful get all users !!")
    }
    users
  }

  /* sona */
  /* TO GET MEMBER BY ID */
  override def getMemberById(id: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("_id" -> BSONObjectID.parse(id).get)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get user by id")
    }
    user
  }
  /* helper function */
  override def blockMemberProducts(ids: List[BSONObjectID], status: Int): Future[WriteResult] = {
    var writeRes: Future[WriteResult] = null
    for (id <- ids) {
      writeRes = productCollection.flatMap(_.update(
        Json.obj("_id" -> id), {
          BSONDocument(
            "$set" -> BSONDocument(
              "status" -> status
            )
          )
        }
      ))
    }
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully updated products status")
    }
    writeRes
  }

  /* TO CHANGE MEMBER STATUS */
  override def blockMemberById(id: String, status: Int): Future[WriteResult] = {
    def filter: BSONDocument = {
      if (status == -1) {
        val promoted = storeCollection.flatMap(_.find(Json.obj("userId" -> BSONObjectID.parse(id).get))
          .projection(
            Json.obj(
              "_id" -> 0,
              "productId" -> 1
            )
          )
          .cursor[JsObject](ReadPreference.primary)
          .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
        promoted.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(data) => {
            val productIds = (data.head \ "productId").as[List[BSONObjectID]]
            println("Data: " + productIds)
            blockMemberProducts(productIds, status)
          } // close case success
        }
        // set user status
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> status,
            "dateBlock" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis
          )
        )
      } else {
        val promoted = storeCollection.flatMap(_.find(Json.obj("userId" -> BSONObjectID.parse(id).get))
          .projection(
            Json.obj(
              "_id" -> 0,
              "productId" -> 1
            )
          )
          .cursor[JsObject](ReadPreference.primary)
          .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
        promoted.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(data) => {
            val productIds = (data.head \ "productId").as[List[BSONObjectID]]
            println("Data: " + productIds)
            blockMemberProducts(productIds, status)
          } // close case success
        }
        // set user status
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> status,
            "dateBlock" -> -25200000
          )
        )
      }
    }

    val writeRes = collection.flatMap(_.update(Json.obj("_id" -> BSONObjectID.parse(id).get), filter))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful updated member's status")
    }
    writeRes
  }

  /* naseat */
  override def blockMerchantById(id: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> -1,
          "dateBlock" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis //Api.parseDate(Api.printDate(new Date())).getTime
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Merchant has been blocked")
    }
    writeRes
  }

  override def getMerchantById(id: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("_id" -> BSONObjectID.parse(id).get)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("successfully find merchant")
    }
    user
  }

  /* Chivon */
  override def getFilterMemberRepo(name: String, location: String, status: Int, fromDate: DateTime, toDate: DateTime, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    try {
      collection.flatMap(_.aggregate(
        Project(
          Json.obj(
            "_id" -> 0,
            "mistakeId" -> 1,
            "socialId" -> 1,
            "firstName" -> 1,
            "name" -> Json.obj("$toLower" -> "$firstName"),
            "city" -> 1,
            "location" -> Json.obj("$toLower" -> "$city"),
            "lastName" -> 1,
            "phone" -> 1,
            "email" -> 1,
            "password" -> 1,
            "verifiedCode" -> 1,
            "otherPhones" -> 1,
            "city" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "type" -> Json.obj("$toLower" -> "$userType"),
            "createdDate" -> Json.obj("$dateToString" -> Json.obj("format" -> "%Y-%m-%d", "date" -> "$dateJoin")),
            "profileImage" -> 1,
            "status" -> 1,
            "online" -> 1
          )
        ), List(
          Match(
            Json.obj("$or" -> List(
              Json.obj(
                "$project" -> List {
                  Json.obj(
                    "$cond" -> List(
                      Json.obj("$not" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))),
                      Json.obj("name" -> Json.obj("$regex" -> "")),
                      Json.obj("name" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")))
                    )
                  )
                }
              ),
              Json.obj(
                "$project" -> List {
                  Json.obj(
                    "$cond" -> List(
                      Json.obj("$not" -> Json.obj("$regex" -> ("^" + location.toLowerCase + ".*"))),
                      Json.obj("location" -> Json.obj("$regex" -> "")),
                      Json.obj("location" -> Json.obj("$regex" -> ("^" + location.toLowerCase + ".*")))
                    )
                  )
                }
              ),
              Json.obj(
                "$project" -> List {
                  Json.obj(
                    "$cond" -> List(
                      Json.obj("$not" -> Json.obj("status" -> status)),
                      Json.obj("status" -> Json.obj("status" -> 1)),
                      Json.obj("status" -> Json.obj("status" -> status))
                    )
                  )
                }
              ),
              Json.obj("$lte" -> List(
                Json.obj("createdDate" -> fromDate)
              )),
              Json.obj("$gte" -> List(
                Json.obj("createdDate" -> toDate)
              ))
            ))
          ),
          Match(Json.obj("type" -> "normal")),
          Sort(Descending(name)),
          Limit(limit),
          Skip((page - 1) * limit),
          Project(
            Json.obj(
              "_id" -> 0,
              "mistakeId" -> 1,
              "socialId" -> 1,
              "firstName" -> 1,
              "lastName" -> 1,
              "phone" -> 1,
              "email" -> 1,
              "password" -> 1,
              "verifiedCode" -> 1,
              "otherPhones" -> 1,
              "city" -> 1,
              "address" -> 1,
              "userType" -> 1,
              "createdDate" -> 1,
              "profileImage" -> 1,
              "status" -> 1,
              "online" -> 1
            )
          )
        )
      ).map(_.head[JsObject]))
    } catch {
      case _: Throwable => Future(null)
    }
  }

  override def getMember(name: String, limit: Int, location: String, status: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    try {
      collection.flatMap(_.aggregate(
        Project(
          Json.obj(
            "_id" -> 0,
            "mistakeId" -> 1,
            "socialId" -> 1,
            "firstName" -> 1,
            "name" -> Json.obj("$toLower" -> "$firstName"),
            "city" -> 1,
            "location" -> Json.obj("$toLower" -> "$city"),
            "lastName" -> 1,
            "phone" -> 1,
            "email" -> 1,
            "password" -> 1,
            "verifiedCode" -> 1,
            "otherPhones" -> 1,
            "city" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "type" -> Json.obj("$toLower" -> "$userType"),
            "createdDate" -> 1,
            "profileImage" -> 1,
            "status" -> 1,
            "online" -> 1
          )
        ), List(
          Match(
            Json.obj("$or" -> List(
              Json.obj("name" -> Json.obj("$regex" -> ("^" + name + ".*"))),
              Json.obj("location" -> Json.obj("$regex" -> ("^" + location + ".*"))),
              Json.obj("status" -> status)
            ))
          ),
          Match(Json.obj("type" -> "normal")),
          Limit(limit),
          Project(
            Json.obj(
              "_id" -> 0,
              "mistakeId" -> 1,
              "socialId" -> 1,
              "firstName" -> 1,
              "lastName" -> 1,
              "phone" -> 1,
              "email" -> 1,
              "password" -> 1,
              "verifiedCode" -> 1,
              "otherPhones" -> 1,
              "city" -> 1,
              "address" -> 1,
              "userType" -> 1,
              "createdDate" -> 1,
              "profileImage" -> 1,
              "status" -> 1,
              "online" -> 1
            )
          )
        )
      ).map(_.head[JsObject]))
    } catch {
      case _: Throwable => Future(null)
    }
  }

  override def updateMember(id: BSONObjectID, user: User): Future[Boolean] = {
    try {
      collection.flatMap(_.update(obj("_id" -> id), user).map {
        case res: WriteResult if res.n > 0 => true
        case res: WriteResult if res.n < 1 => false
      })
    } catch {
      case _: Throwable => Future(false)
    }
  }

  override def deleteMember(id: BSONObjectID): Future[Boolean] = {
    try {
      collection.flatMap(_.remove(obj("_id" -> id))).map {
        case res: WriteResult if res.n > 0 => true
        case res: WriteResult if res.n < 1 => false
      }
    } catch {
      case _: Throwable => Future(false)
    }

  }

  override def getUserWithStoreInfoRepo(username: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val user = collection.flatMap(_.aggregate(
      Lookup(
        "store_tbl",
        "_id",
        "userId",
        "store"
      ),
      List(
        UnwindField("store"),
        Match(Json.obj("userName" -> username)),
        Project(
          Json.obj(
            "_id" -> 0,
            "mistakeId" -> 1,
            "socialId" -> 1,
            "userName" -> 1,
            "phone" -> 1,
            "email" -> 1,
            "password" -> 1,
            "verifiedCode" -> 1,
            "otherPhones" -> 1,
            "city" -> 1,
            "address" -> 1,
            "userType" -> 1,
            "dateJoin" -> 1,
            "dateBlock" -> 1,
            "profileImage" -> 1,
            "status" -> 1,
            "online" -> 1,
            "storeName" -> "$store.storeName",
            "storeInformation" -> "$store.storeInformation",
            "storeBanner" -> "$store.storeBanner",
            "storeUrl" -> "$store.storeUrl",
            "latitude" -> "$store.latitude",
            "longitude" -> "$store.longitude"
          )
        )
      )
    ).map(_.head[JsObject]))
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get user and store information  by username with result $data")
    }
    user
  }

  /*upload image */
  override def uploadImage(id: String, image: String): Future[WriteResult] = {
    def writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "profileImage" -> image
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Image has been uploaded")
    }
    writeRes
  }

  /*update profile*/
  override def updateMemberProfile(id: String, userName: String, phone: String, email: String, phones: List[String], location: String, address: String): Future[WriteResult] = {
    def filter = {
      if (userName != "") {
        BSONDocument(
          "$set" -> BSONDocument(
            "userName" -> userName
          )
        )
      } else if (email != "") {
        BSONDocument(
          "$set" -> BSONDocument(
            "email" -> email
          )
        )
      } else if (phone != "") {
        BSONDocument(
          "$set" -> BSONDocument(
            "phone" -> phone
          )
        )
      } else {
        BSONDocument(
          "$set" -> BSONDocument(
            "otherPhones" -> phones,
            "city" -> location,
            "address" -> address
          )
        )
      }
    }
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get), filter
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful updated member")
    }
    writeRes
  }

  override def changePassword(id: String, newPassword: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "password" -> newPassword
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully update new password")
    }
    writeRes
  }

  override def countAllNormalMembers(): Future[Int] = {
    val members = collection.flatMap(_.find(Json.obj("userType" -> "normal"))
      .cursor[User](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[User]]()))
    members.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get product count !!")
    }
    members.map(normal => normal.length)
  }

  override def countAllMerchantMembers(): Future[Int] = {
    val members = collection.flatMap(_.find(Json.obj("userType" -> "merchant"))
      .cursor[User](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[User]]()))
    members.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get product count !!")
    }
    members.map(normal => normal.length)
  }
}

