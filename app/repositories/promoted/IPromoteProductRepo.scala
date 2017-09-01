package repositories.promoted

import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.promoted.{ PromoteProduct }
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
 * Created by Ky Sona on 3/9/2017.
 */
@ImplementedBy(classOf[PromoteProductRepo])
trait IPromoteProductRepo {
  def listUserPromoteProduct(location: String, dateStart: String, dateEnd: String, name: String, page: Int, limit: Int): Future[List[JsObject]]
  def listPromoteProductByUserId(promoteId: String, userId: String): Future[List[JsObject]]
  def approveProductById(ids: List[String], promoteType: String): Future[WriteResult]
  def approvedPromotedRequestById(id: String): Future[WriteResult]
  def deleteUserRequestById(id: String): Future[WriteResult]
  def deleteProductRequestById(productId: String): Future[WriteResult]
  def updatePriceById(id: String, price: Double): Future[WriteResult]
  def insertPromoteProducts(promoted: PromoteProduct): Future[WriteResult]
  def listPromotedProductsExpired(page: Int, limit: Int): Future[List[JsObject]]
  def updateProductTypeTobeNormal(_id: BSONObjectID, ids: List[BSONObjectID]): Future[WriteResult]
  def updatePromotedProductExpired(): Future[List[JsObject]]
}

class PromoteProductRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IPromoteProductRepo {
  def userCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))

  def promotedProductCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_product_tbl"))

  def promotedPackageCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("promoted_package_tbl"))

  def productCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("product_tbl"))

  def matchFilter(location: String, dateStart: String, dateEnd: String, name: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(dateStart).getMillis
    val toDateLong = FormatDate.parseDate(dateEnd).getMillis

    if ((fromDateLong > 0 && toDateLong > 0) && name == "" && location == "") {
      println("Date")
      Json.obj(
        "status" -> 0,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if (location != "" && name == "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Location")
      Json.obj(
        "status" -> 0,
        "promote_users.city" -> location
      )
    } else if (name != "" && location == "" && (fromDateLong > 0 && toDateLong > 0)) {
      println("Name")
      Json.obj(
        "status" -> 0,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && location != "" && name == "") {
      println("Date Location")
      Json.obj(
        "status" -> 0,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "promote_users.city" -> location
      )
    } else if ((fromDateLong > 0 && toDateLong > 0) && name != "" && location == "") {
      println("Date Name")
      Json.obj(
        "status" -> 0,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else {
      println("Date Location Name")
      Json.obj(
        "status" -> 0,
        "startDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "promote_users.city" -> location,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    }
  }

  /* Helper Function */
  def getCountUsers(location: String, dateStart: String, dateEnd: String, name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "promoted_package_tbl",
        "packageId",
        "_id",
        "promote_types"
      ),
      List(
        UnwindField("promote_types"),
        Lookup(
          "user_tbl",
          "userId",
          "_id",
          "promote_users"
        ),
        UnwindField("promote_users"),
        Project(
          Json.obj(
            "_id" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "promote_users.userName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$promote_users.userName"),
            "promote_users.city" -> 1
          )
        ),
        Match(matchFilter(location, dateStart, dateEnd, name)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listUserPromoteProduct(location: String, dateStart: String, dateEnd: String, name: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountUsers(location, dateStart, dateEnd, name), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Product Count" + total)
    val users = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "promote_users"
      ),
      List(
        UnwindField("promote_users"),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "promote_users._id" -> 1,
            "promote_users.profileImage" -> 1,
            "promote_users.userName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$promote_users.userName"),
            "promote_users.city" -> 1,
            "typePromote" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "productId" -> 1,
            "total_products" -> Json.obj("$size" -> "$productId"),
            "startDate" -> 1,
            "endDate" -> 1,
            "price" -> 1,
            "status" -> 1,
            "total" -> s"$total"
          )
        ),
        Match(matchFilter(location, dateStart, dateEnd, name)),
        Sort(Ascending("lowName")),
        Skip((page * limit) - limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all users promote products")
    }
    users
  }

  def getCountProducts(userId: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "promote_product"
      ),
      List(
        UnwindField("promote_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1
          )
        ),
        Match(Json.obj("userId" -> BSONObjectID.parse(userId).get)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listPromoteProductByUserId(promoteId: String, userId: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountProducts(userId), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Product Count" + total)
    val products = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "promote_product"
      ),
      List(
        UnwindField("promote_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "userId" -> 1,
            "typePromote" -> 1,
            "duration" -> 1,
            "price" -> 1,
            "startDate" -> 1,
            "endDate" -> 1,
            "status" -> 1,
            "promote_product._id" -> 1,
            "promote_product.productName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$promote_product.productName"),
            "promote_product.productImage" -> 1,
            "promote_product.price" -> 1,
            "promote_product.discount" -> 1,
            "promote_product.productType" -> 1,
            "promote_product.createDate" -> 1,
            "promote_product.expireDate" -> 1,
            "promote_product.status" -> 1,
            "total" -> s"$total"
          )
        ),
        Match(Json.obj(
          "_id" -> BSONObjectID.parse(promoteId).get,
          "userId" -> BSONObjectID.parse(userId).get,
          "status" -> 0,
          "promote_product.status" -> 1

        )),
        Sort(Ascending("lowName"))
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all user promote products")
    }
    products
  }

  override def approveProductById(ids: List[String], promoteType: String): Future[WriteResult] = {
    var writeRes: Future[WriteResult] = null
    for (id <- ids) {
      writeRes = productCollection.flatMap(_.update(
        Json.obj("_id" -> BSONObjectID.parse(id).get), {
          BSONDocument(
            "$set" -> BSONDocument(
              "productType" -> promoteType
            )
          )
        }
      ))
    }
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Product updated product")
    }
    writeRes
  }

  override def approvedPromotedRequestById(id: String): Future[WriteResult] = {
    val writeRes = promotedProductCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get), {
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> 1
          )
        )
      }
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully accept user request")
    }
    writeRes
  }

  override def deleteUserRequestById(id: String): Future[WriteResult] = {
    val writeRes = promotedProductCollection.flatMap(_.remove(obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully delete user request")
    }
    writeRes
  }

  override def deleteProductRequestById(productId: String): Future[WriteResult] = {
    val writeRes = promotedProductCollection.flatMap(_.update(
      Json.obj(),
      Json.obj("$pull" -> Json.obj("productId" -> BSONObjectID.parse(productId).get)),
      multi = true
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully deleted product request by product ID")
    }
    writeRes
  }

  override def updatePriceById(id: String, price: Double): Future[WriteResult] = {
    val writeRes = promotedProductCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "price" -> price
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully update price")
    }
    writeRes
  }

  override def insertPromoteProducts(promoted: PromoteProduct): Future[WriteResult] = {
    val writeRes = promotedProductCollection.flatMap(_.insert(promoted))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new promoted products")
    }
    writeRes
  }

  // helper functions
  def getCountProductsExpired(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val total = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "products"
      ),
      List(
        UnwindField("products"),
        Project(
          Json.obj(
            "_id" -> 1,
            "status" -> 1,
            "endDate" -> 1,
            "products.status" -> 1
          )
        ),
        Match(Json.obj(
          "status" -> 1,
          "endDate" -> Json.obj("$lt" -> nowLong),
          "products.status" -> 1
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
  override def listPromotedProductsExpired(page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val jsObj: List[JsObject] = Await.result(getCountProductsExpired(), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Product Count" + total)
    val promoted = promotedProductCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "products"
      ),
      List(
        Match(Json.obj(
          "status" -> 1,
          "endDate" -> Json.obj("$lt" -> nowLong),
          "products.status" -> 1
        )),
        UnwindField("products"),
        Project(
          Json.obj(
            "_id" -> 0,
            //"productId" -> 1,
            "products.productName" -> 1,
            "products.productImage" -> 1,
            "products.price" -> 1,
            "products.discount" -> 1,
            "products.productType" -> 1,
            "products.createDate" -> 1,
            "products.expireDate" -> 1,
            "products.status" -> 1,
            "total" -> s"$total"
          )
        ),
        Skip((page * limit) - limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    promoted.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all expired products")
    }
    promoted
  }

  override def updateProductTypeTobeNormal(_id: BSONObjectID, ids: List[BSONObjectID]): Future[WriteResult] = {
    val updateId = promotedProductCollection.flatMap(_.update(
      Json.obj("_id" -> _id),
      {
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> -1
          )
        )
      }
    ))
    updateId.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => {
        println("Successfully update promtoed product status");
        var writeRes: Future[WriteResult] = null
        for (id <- ids) {
          writeRes = productCollection.flatMap(_.update(
            Json.obj("_id" -> id, "status" -> 1), {
              BSONDocument(
                "$set" -> BSONDocument(
                  "productType" -> "normal"
                )
              )
            }
          ))
        }
        writeRes.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(writeResult) => println("Product updated product")
        }
        writeRes
      } // close success case
    }
    updateId
  }

  override def updatePromotedProductExpired(): Future[List[JsObject]] = {
    val nowLong = FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis;
    val promoted = promotedProductCollection.flatMap(_.find(Json.obj("endDate" -> Json.obj("$lt" -> nowLong), "status" -> 1))
      .projection(
        Json.obj(
          "_id" -> 1,
          "productId" -> 1
        )
      )
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
    promoted.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => {
        for (p <- data) {
          println("data:" + p)
          val _id = (p \ "_id").as[BSONObjectID]
          val productIds = (p \ "productId").as[List[BSONObjectID]]
          updateProductTypeTobeNormal(_id, productIds)
        }
      } // close case success
    }
    promoted
  }

}
