package repositories.product

import com.google.inject.{ ImplementedBy, Inject }
import models.category.Category
import models.store.Store
import models.product.Product
import org.joda.time.DateTime
import play.api.libs.json.Json._
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{ Cursor, QueryOpts, ReadPreference }
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import utils.FormatDate

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.parsing.json.JSONObject
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[ProductRepo])
trait IProductRepo {

  def insertProduct(product: Product): Future[WriteResult]

  def getProductById(productId: String): Future[Option[Product]]

  def uploadProductImage(productId: String, image: List[String]): Future[WriteResult]

  def removeProductImage(productId: String, image: String): Future[WriteResult]

  def renewProductById(productId: String): Future[WriteResult]

  def deleteProductById(productId: String): Future[WriteResult]

  def updateProductById(productId: String, productName: String, productDescription: String, price: Double): Future[WriteResult]

  /* sona */
  def listProducts(location: String, productType: String, status: Int, dateStart: String, dateEnd: String,
    startPrice: Double, endPrice: Double, name: String, page: Int, limit: Int): Future[List[JsObject]]
  def updateStatusProductById(id: String, status: Int): Future[WriteResult]
  def getProductInfoById(id: String): Future[List[JsObject]]
  def listPromoteProductsByUserId(userId: String): Future[List[JsObject]]
  def getPromotedProductByProductId(id: String): Future[Option[Product]]

  /*chivon*/
  def getProductByCategoryName(checkCategory: Int, categoryName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductByMultiFilterWithCategory(checkCategory: Int, categoryName: String, subCategoryName: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getProductByLocationFilter(productType: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getProductsByNameAndCategoryName(checkCategory: Int, categoryName: String, productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductsByName(productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductByMultiFilterWithLocation(productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getSubCategoryRepo(categoryName: String): Future[List[JsObject]]
  def getProductRelatedRepo(checkCategory: Int, categoryName: String, productId: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductRecentlyRepo(start: Int, limit: Int): Future[List[JsObject]]
  def getProductByUserNameRepo(username: String, start: Int, limit: Int): Future[List[JsObject]]

  def listRelatedProducts(cat_id: String, pro_id: String): Future[List[JsObject]]
  def listRecentlyProducts(): Future[List[JsObject]]

  /*oudam*/
  def getProductByUserId(userId: String, start: Int, limit: Int): Future[List[JsObject]]
  def updateProductStatus(productId: String, status: Int): Future[WriteResult]
  def getProductByProductId(productId: String): Future[List[JsObject]]
  def updateProductInfo(product: Product): Future[WriteResult]
  /* naseat */
  def searchProductByName(checkCategory: Int, categoryName: String, productName: String): Future[List[JsObject]]
  def listProductsReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, userName: String,
    userType: String, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def updateProductStatusById(id: String, status: Int): Future[WriteResult]

  /* sopheak */
  def countTodayProducts(): Future[Int]

}

class ProductRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IProductRepo {
  def storeCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("store_tbl"))

  def userCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))

  def productCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("product_tbl"))

  def categoryCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("category_tbl"))

  def getCountListProductRepo(item: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = productCollection.flatMap(_.aggregate(
      Match(Json.obj("productType" -> item)),
      List(
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count product")
    }
    total
  }

  def getCountProductByCategory(name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = productCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "productId",
        "category"
      ),
      List(
        UnwindField("category"),
        Match(Json.obj("category.categoryName" -> name)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count product by category")
    }
    total
  }

  override def insertProduct(product: Product): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.insert(product))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new product")
    }
    writeRes
  }

  override def getProductById(productId: String): Future[Option[Product]] = {
    val products = productCollection.flatMap(_.find(obj("_id" -> BSONObjectID.parse(productId).get)).one[Product])
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful find product by productId")
    }
    products
  }

  override def uploadProductImage(productId: String, image: List[String]): Future[WriteResult] = {
    def writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "productImage" -> image
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Image has been uploaded")
    }
    writeRes
  }

  override def removeProductImage(productId: String, image: String): Future[WriteResult] = {
    def writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$pull" -> BSONDocument(
          "productImage" -> image
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Image has been removed")
    }
    writeRes
  }

  override def renewProductById(productId: String): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "createDate" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully renew product")
    }
    writeRes
  }

  override def deleteProductById(productId: String): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.remove(obj("_id" -> BSONObjectID.parse(productId).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully delete product")
    }
    writeRes
  }

  override def updateProductById(productId: String, productName: String, productDescription: String, price: Double): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "productName" -> productName,
          "productDescription" -> productDescription,
          "price" -> price
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully update product")
    }
    writeRes
  }

  /* sona */
  def matchFilter(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, name: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(dateStart).getMillis
    val toDateLong = FormatDate.parseDate(dateEnd).getMillis

    if ((status == 0 || status == 1 || status == -1) && location != "" && name == "" && productType == ""
      && (fromDateLong < 0 && toDateLong < 0) && (startPrice < 0 || endPrice < 0)) {
      println("Status Location")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && location == "" && name == ""
      && (fromDateLong < 0 && toDateLong < 0) && (startPrice < 0 || endPrice < 0)) {
      println("Status Type")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && productType == ""
      && location == "" && name == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && location == "" && name == "") {
      println("Status Price")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && name != "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Name")
      Json.obj(
        "store_product.status" -> status,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && name == "" && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Location Type")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && productType == "" && name == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Location Date")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && name == "") {
      println("Status Location Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && name != ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Location Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (fromDateLong > 0 && toDateLong > 0)
      && location == "" && name == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Type Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && location == "" && name == "") {
      println("Status Type Price")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && name != ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && location == "") {
      println("Status Type Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0)
      && productType == "" && name == "" && location == "") {
      println("Status Date Price" + startPrice + "/" + endPrice)
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && name != ""
      && (startPrice < 0 || endPrice < 0) && productType == "" && location == "") {
      println("Status Date Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0) && name != ""
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && location == "") {
      println("Status Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice < 0 || endPrice < 0) && name == "") {
      println("Status Location Type Date")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && (startPrice > 0 || endPrice > 0) && productType == "" && name == "") {
      println("Status Location Date Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && name != "" && (startPrice < 0 || endPrice < 0) && productType == "") {
      println("Status Location Date Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (startPrice > 0 || endPrice > 0)
      && name != "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Location Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (startPrice > 0 || endPrice > 0) && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("Status Location Type Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && name == "" && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Location Type Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && name != "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Type Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong > 0 && toDateLong > 0) && name == "" && location == "") {
      println("Status Type Price Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && name == "") {
      println("Status Type Price Location")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.city" -> location
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0)
      && (fromDateLong > 0 && toDateLong > 0) && name != "" && location == "" && productType == "") {
      println("Status Date Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0) && name == "") {
      println("Status Location Type Date Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && name != "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Location Type Date Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && name != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0) && productType == "") {
      println("Status Location Date Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0) && name != "") {
      println("All")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*"))
      )
    } else {
      println("Default is Status")
      Json.obj(
        "store_product.status" -> status
      )
    }
  }

  /* Helper Function */
  def getCountProducts(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = storeCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "store_user"
      ),
      List(
        UnwindField("store_user"),
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "store_product"
        ),
        UnwindField("store_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$store_product.productName"),
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user.city" -> 1
          )
        ),
        Match(matchFilter(location, productType, status, dateStart, dateEnd, startPrice, endPrice, name)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listProducts(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, name: String, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountProducts(location, productType, status, dateStart, dateEnd, startPrice, endPrice, name), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Product Count" + total)
    val products = storeCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "store_user"
      ),
      List(
        UnwindField("store_user"),
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "store_product"
        ),
        UnwindField("store_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$store_product.productName"),
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user._id" -> 1,
            "store_user.city" -> 1,
            "store_user.userName" -> 1
          )
        ),
        Match(matchFilter(location, productType, status, dateStart, dateEnd, startPrice, endPrice, name)),
        Sort(Ascending("lowName")),
        Skip((page * limit) - limit),
        Limit(limit),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user._id" -> 1,
            "store_user.city" -> 1,
            "store_user.userName" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all products")
    }
    products
  }

  override def updateStatusProductById(id: String, status: Int): Future[WriteResult] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: BSONDocument = {
      if (status == -1) {
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> status,
            "blockDate" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis
          )
        )
      } else {
        BSONDocument(
          "$set" -> BSONDocument(
            "status" -> status,
            "blockDate" -> -25200000
          )
        )
      }
    }
    val writeRes = productCollection.flatMap(_.update(Json.obj("_id" -> BSONObjectID.parse(id).get), filter))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => {
        // get userId and productId
        val products = storeCollection.flatMap(_.aggregate(
          Match(Json.obj("productId" -> BSONObjectID.parse(id).get)),
          List(
            Project(
              Json.obj(
                "_id" -> 1,
                "userId" -> 1,
                "productId" -> 1
              )
            )
          )
        ).map(_.head[JsObject]))
        products.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(data) => {
            val userId = (data.head \ "userId").as[BSONObjectID]
            val productId = (data.head \ "productId").as[List[BSONObjectID]]
            var count: Int = 0;
            for (id <- productId) {
              val products = productCollection.flatMap(_.find(Json.obj("_id" -> id, "status" -> -1)).one[Product])
              products.onComplete {
                case Failure(e) => e.printStackTrace()
                case Success(data) => {
                  if (data.nonEmpty) {
                    count = count + 1
                  }
                }
              }
            } // close for
            if (count >= 3) {
              println("BLOCK")
              def blockUser = userCollection.flatMap(_.update(
                Json.obj("_id" -> userId),
                BSONDocument(
                  "$set" -> BSONDocument(
                    "status" -> -1
                  )
                )
              ))
              blockUser.onComplete {
                case Failure(e) => e.printStackTrace()
                case Success(writeResult) => println("Successfully block user")
              }
            } else {
              println("UNBLOCK")
              def blockUser = userCollection.flatMap(_.update(
                Json.obj("_id" -> userId),
                BSONDocument(
                  "$set" -> BSONDocument(
                    "status" -> 1
                  )
                )
              ))
              blockUser.onComplete {
                case Failure(e) => e.printStackTrace()
                case Success(writeResult) => println("Successfully unblock user")
              }
            }
          } // close case success
        } // close on completed
      } // close success case
    }
    writeRes
  }

  override def getProductInfoById(id: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val products = storeCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "store_user"
      ),
      List(
        UnwindField("store_user"),
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "store_product"
        ),
        UnwindField("store_product"),
        Lookup(
          "product_view_tbl",
          "store_product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "category_tbl",
          "store_product._id",
          "productId",
          "categories"
        ),
        UnwindField("categories"),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user.userName" -> 1,
            "store_user.phone" -> 1,
            "store_user.email" -> 1,
            "store_user.otherPhones" -> 1,
            "store_user.city" -> 1,
            "categories.categoryName" -> 1,
            "categories._id" -> 1,
            "views" -> Json.obj("$sum" -> "$view.viewer.views")
          )
        ),
        Match(Json.obj("store_product._id" -> BSONObjectID.parse(id).get))
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get product information")
    }
    products
  }

  override def listPromoteProductsByUserId(userId: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val products = userCollection.flatMap(_.aggregate(
      Lookup(
        "store_tbl",
        "_id",
        "userId",
        "store"
      ),
      List(
        UnwindField("store"),
        Lookup(
          "product_tbl",
          "store.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Match(Json.obj("_id" -> BSONObjectID.parse(userId).get, "product.status" -> 1)),
        Sort(Descending("product.createDate")),
        Project(
          Json.obj(
            "_id" -> 0,
            "id" -> "$product._id",
            "name" -> "$product.productName",
            "description" -> "$product.productDescription",
            "price" -> "$product.price",
            "type" -> "$product.productType",
            "discount" -> "$product.discount",
            "discountFromDate" -> "$product.discountFromDate",
            "discountEndDate" -> "$product.discountEndDate",
            "images" -> "$product.productImage",
            "createDate" -> "$product.createDate",
            //"store" -> "$store.productId",
            "view" -> Json.obj("$sum" -> "$view.viewer.views")
          )
        )
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get products by userId with result $data")
    }
    products
  }

  override def getPromotedProductByProductId(id: String): Future[Option[Product]] = {
    productCollection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get)).one[Product])
  }

  /*chivon*/
  override def getProductByCategoryName(checkCategory: Int, categoryName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (checkCategory > 0) {
        Json.obj(
          "categoryName" -> categoryName,
          "status" -> 1
        )
      } else {
        Json.obj(
          "subCategoryName" -> categoryName,
          "status" -> 1
        )
      }
    }
    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    if (checkCategory > 0) {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> "$subCategoryName"
          )
        ),
        "total" -> SumAll
      )
    } else {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> ""
          )
        ),
        "total" -> SumAll
      )
    }

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Sort(Descending("productType")),
        Sort(Descending("createDate")),
        Match(filter),
        group,
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products multiple filter with category !!")
    }
    products
  }

  override def listRelatedProducts(cat_id: String, pro_id: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    productCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "productId",
        "in_category"
      ),
      List(
        Match(Json.obj(
          "in_category._id" -> BSONObjectID.parse(cat_id).get,
          "_id" -> Json.obj("$ne" -> BSONObjectID.parse(pro_id).get)
        )),
        Limit(6),
        Sort(Descending("createDate")),
        Project(
          Json.obj(
            "_id" -> 1,
            "productName" -> 1,
            "productImage" -> 1,
            "price" -> 1,
            "discount" -> 1,
            "createDate" -> 1,
            "in_category" -> Json.obj(
              "id" -> "$in_category._id",
              "categoryName" -> "$in_category.categoryName"
            )
          )
        )
      )
    ).map(_.head[JsObject]))
  }

  override def listRecentlyProducts(): Future[List[JsObject]] = {
    val sort: Int = -1
    val genericQueryBuilder = productCollection.map(_.find(Json.obj()).sort(Json.obj("createDate" -> sort)).options(QueryOpts(0)))
    val cursor = genericQueryBuilder.map(_.cursor[JsObject](ReadPreference.Primary))
    cursor.flatMap(_.collect[List](6, Cursor.FailOnError[List[JsObject]]()))
  }

  def matchFilterProduct(checkCategory: Int, categoryName: String, subCategoryName: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double): JsObject = {
    /** Date Rang == 0, 1, 2, 3, 4 (Any, Today, This Week, This Month, This Year) **/
    val today = new DateTime()
    var startDate: Long = 0
    var endDate: Long = 0
    if (dateRang == 1) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
    } else if (dateRang == 2) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMaximumValue())).getMillis
    } else if (dateRang == 3) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMaximumValue())).getMillis
    } else if (dateRang == 4) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMaximumValue())).getMillis
    } else {
      println("No date rang")
      startDate = 0
      endDate = 0
    }
    if (checkCategory > 0) {
      // Have sub category
      /* ================ sub pro ==========*/
      if (subCategoryName != "" && productName == "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub Category : " + subCategoryName)
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "subCategoryName" -> subCategoryName,
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category and product name")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name and location")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name, location and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, location, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, location and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /* ================ sub location ==========*/ else if (subCategoryName != "" && productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category and location")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, location and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, location, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, location, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Sub date rang=================*/ else if (subCategoryName != "" && productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName != "" && productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Sub price rang=================*/ else if (subCategoryName != "" && productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "subCategoryName" -> subCategoryName,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Name=================*/ else if (subCategoryName == "" && productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Product name")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Product name and location")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Product name, location and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, location, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, location and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Product name and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, location, date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Location=================*/ else if (subCategoryName == "" && productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Location")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Location and date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Location, date rang and price")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Location and price")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Date rang=================*/ else if (subCategoryName == "" && productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Date rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subCategoryName == "" && productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=============Price rang=================*/ else if (subCategoryName == "" && productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Date rang and price rang")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else {
        println("DEFAULT")
        Json.obj(
          "categoryName" -> categoryName,
          "productType" -> productType,
          "status" -> 1
        )
      }
    } else {
      // No sub category
      if (productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and Location")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and date rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name, date rang and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => ProductName , location and date rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => ProductName , location and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => ProductName , location, date rang and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=================Location======================*/ else if (productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Location")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Location and date rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location, date rang and price rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=================Date rang======================*/ else if (productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Date rang")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Date rang and price") // Filter No sub category => Date rang
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*=================Price rang======================*/ else if (productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Price") // Filter No sub category => Price
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else {
        println("No sub category => DEFAULT")
        Json.obj(
          "subCategoryName" -> categoryName,
          "productType" -> productType,
          "status" -> 1
        )
      }
    }
  }

  override def getProductByMultiFilterWithCategory(checkCategory: Int, categoryName: String, subCategoryName: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "userName" -> "$user.userName",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    if (checkCategory > 0) {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "userName" -> "$userName",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> "$subCategoryName"
          )
        ),
        "total" -> SumAll
      )
    } else {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "userName" -> "$userName",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> ""
          )
        ),
        "total" -> SumAll
      )
    }

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Match(matchFilterProduct(checkCategory, categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice)),
        group,
        Sort(Descending("createDate")),
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((page - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    /** productType == hot or gold **/
    if (productType != "normal") {
      products = categoryCollection.flatMap(_.aggregate(
        Lookup(
          "category_tbl",
          "_id",
          "parentId",
          "sub"
        ),
        List(
          UnwindField("sub"),
          Lookup(
            "product_tbl",
            "sub.productId",
            "_id",
            "product"
          ),
          UnwindField("product"),
          Lookup(
            "product_view_tbl",
            "product._id",
            "productId",
            "view"
          ),
          UnwindField("view"),
          Lookup(
            "store_tbl",
            "product._id",
            "productId",
            "store"
          ),
          UnwindField("store"),
          Lookup(
            "user_tbl",
            "store.userId",
            "_id",
            "user"
          ),
          UnwindField("user"),
          project,
          Match(matchFilterProduct(checkCategory, categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice)),
          Sample(Int.MaxValue),
          group,
          UnwindField("product"),
          Project(
            Json.obj(
              "product" -> 1,
              "total" -> 1
            )
          ),
          Skip((page - 1) * limit),
          Limit(limit)
        )
      ).map(_.head[JsObject]))
    }
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products multiple filter with category !!")
    }
    products
  }

  override def getProductByLocationFilter(productType: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val today = new DateTime()
    var startDate: Long = 0
    var endDate: Long = 0
    if (dateRang == 1) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
    } else if (dateRang == 2) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMaximumValue())).getMillis
    } else if (dateRang == 3) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMaximumValue())).getMillis
    } else if (dateRang == 4) {
      startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMinimumValue())).getMillis
      endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMaximumValue())).getMillis
    } else {
      println("No date rang")
      startDate = 0
      endDate = 0
    }
    def filter: JsObject = {
      if (dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Date rang")
        Json.obj(
          "product.productType" -> productType,
          "user.city" -> location,
          "product.createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate)
        )
      } else if (dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Price rang")
        Json.obj(
          "product.productType" -> productType,
          "user.city" -> location,
          "product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
        )
      } else if (dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Date rang and price rang")
        Json.obj(
          "product.productType" -> productType,
          "user.city" -> location,
          "product.createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
        )
      } else {
        Json.obj(
          "product.productType" -> productType,
          "user.city" -> location
        )
      }
    }

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "product"
      ),
      List(
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        Match(filter),
        Sort(Descending("product.createDate")),
        Group(JsString("$state"))(
          "product" -> Push(
            Json.obj(
              "id" -> "$product._id",
              "name" -> "$product.productName",
              "description" -> "$product.productDescription",
              "location" -> "$user.city",
              "username" -> "$user.userName",
              "price" -> "$product.price",
              "discount" -> "$product.discount",
              "discountFromDate" -> "$product.discountFromDate",
              "discountEndDate" -> "$product.discountEndDate",
              "images" -> "$product.productImage",
              "createDate" -> "$product.createDate",
              "views" -> Json.obj("$sum" -> "$view.viewer.views"),
              "categoryName" -> "$categoryName",
              "subCategoryName" -> ""
            )
          ),
          "total" -> SumAll
        ),
        Skip((page - 1) * limit),
        Limit(limit),
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        )
      )
    ).map(_.head[JsObject]))

    /** productType == hot or gold **/
    if (productType != "normal") {
      products = categoryCollection.flatMap(_.aggregate(
        Lookup(
          "category_tbl",
          "_id",
          "parentId",
          "sub"
        ),
        List(
          UnwindField("sub"),
          Lookup(
            "product_tbl",
            "sub.productId",
            "_id",
            "product"
          ),
          UnwindField("product"),
          Lookup(
            "product_view_tbl",
            "product._id",
            "productId",
            "view"
          ),
          UnwindField("view"),
          Lookup(
            "store_tbl",
            "product._id",
            "productId",
            "store"
          ),
          UnwindField("store"),
          Lookup(
            "user_tbl",
            "store.userId",
            "_id",
            "user"
          ),
          UnwindField("user"),
          Match(filter),
          Group(JsString("$state"))(
            "product" -> Push(
              Json.obj(
                "id" -> "$product._id",
                "name" -> "$product.productName",
                "description" -> "$product.productDescription",
                "location" -> "$user.city",
                "username" -> "$user.userName",
                "price" -> "$product.price",
                "discount" -> "$product.discount",
                "discountFromDate" -> "$product.discountFromDate",
                "discountEndDate" -> "$product.discountEndDate",
                "images" -> "$product.productImage",
                "createDate" -> "$product.createDate",
                "views" -> Json.obj("$sum" -> "$view.viewer.views"),
                "categoryName" -> "$categoryName",
                "subCategoryName" -> ""
              )
            ),
            "total" -> SumAll
          ),
          Sample(Int.MaxValue),
          Skip((page - 1) * limit),
          Limit(limit),
          UnwindField("product"),
          Project(
            Json.obj(
              "product" -> 1,
              "total" -> 1
            )
          )
        )
      ).map(_.head[JsObject]))
    }

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products location filter with result : " + data)
    }
    products
  }

  override def getProductsByNameAndCategoryName(checkCategory: Int, categoryName: String, productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    /** Match **/
    def filter: JsObject = {
      if (productName != "") {
        if (checkCategory > 0) {
          Json.obj(
            "productType" -> productType,
            "categoryName" -> categoryName,
            "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
            "status" -> 1
          )
        } else {
          Json.obj(
            "productType" -> productType,
            "subCategoryName" -> categoryName,
            "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
            "status" -> 1
          )
        }
      } else {
        if (checkCategory > 0) {
          Json.obj("productType" -> productType, "categoryName" -> categoryName, "status" -> 1)
        } else {
          Json.obj("productType" -> productType, "subCategoryName" -> categoryName, "status" -> 1)
        }
      }
    }
    /** Project **/
    val project = Project(
      Json.obj(
        "product._id" -> 1,
        "productType" -> "$product.productType",
        "product.productName" -> 1,
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "product.productDescription" -> 1,
        "user.city" -> 1,
        "product.price" -> 1,
        "product.discount" -> 1,
        "product.discountFromDate" -> 1,
        "product.discountEndDate" -> 1,
        "product.productImage" -> 1,
        "product.createDate" -> 1,
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    if (checkCategory > 0) {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$product._id",
            "name" -> "$product.productName",
            "description" -> "$product.productDescription",
            "location" -> "$user.city",
            "price" -> "$product.price",
            "discount" -> "$product.discount",
            "discountFromDate" -> "$product.discountFromDate",
            "discountEndDate" -> "$product.discountEndDate",
            "images" -> "$product.productImage",
            "createDate" -> "$product.createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> "$subCategoryName"
          )
        ),
        "total" -> SumAll
      )
    } else {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$product._id",
            "name" -> "$product.productName",
            "description" -> "$product.productDescription",
            "location" -> "$user.city",
            "price" -> "$product.price",
            "discount" -> "$product.discount",
            "discountFromDate" -> "$product.discountFromDate",
            "discountEndDate" -> "$product.discountEndDate",
            "images" -> "$product.productImage",
            "createDate" -> "$product.createDate",
            "views" -> "$views",
            "categoryName" -> "$subCategoryName",
            "subCategoryName" -> ""
          )
        ),
        "total" -> SumAll
      )
    }
    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Match(filter),
        Sort(Descending("product.createDate")),
        group,
        Skip((start - 1) * limit),
        Limit(limit),
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        )
      )
    ).map(_.head[JsObject]))

    /** productType == hot or gold **/
    if (productType != "normal") {
      products = categoryCollection.flatMap(_.aggregate(
        Lookup(
          "category_tbl",
          "_id",
          "parentId",
          "sub"
        ),
        List(
          UnwindField("sub"),
          Lookup(
            "product_tbl",
            "sub.productId",
            "_id",
            "product"
          ),
          UnwindField("product"),
          Lookup(
            "product_view_tbl",
            "product._id",
            "productId",
            "view"
          ),
          UnwindField("view"),
          Lookup(
            "store_tbl",
            "product._id",
            "productId",
            "store"
          ),
          UnwindField("store"),
          Lookup(
            "user_tbl",
            "store.userId",
            "_id",
            "user"
          ),
          UnwindField("user"),
          project,
          Match(filter),
          group,
          Sample(Int.MaxValue),
          Skip((start - 1) * limit),
          Limit(limit),
          UnwindField("product"),
          Project(
            Json.obj(
              "product" -> 1,
              "total" -> 1
            )
          )
        )
      ).map(_.head[JsObject]))
    }

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful product by name and category's name with result : " + data)
    }
    products
  }

  override def getProductsByName(productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._

    val project = Project(
      Json.obj(
        "product._id" -> 1,
        "productType" -> "$product.productType",
        "product.productName" -> 1,
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "product.productDescription" -> 1,
        "user.city" -> 1,
        "product.price" -> 1,
        "product.discount" -> 1,
        "product.discountFromDate" -> 1,
        "product.discountEndDate" -> 1,
        "product.productImage" -> 1,
        "product.createDate" -> 1,
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1
      )
    )
    val group = Group(JsString("$state"))(
      "product" -> Push(
        Json.obj(
          "id" -> "$product._id",
          "name" -> "$product.productName",
          "description" -> "$product.productDescription",
          "location" -> "$user.city",
          "price" -> "$product.price",
          "discount" -> "$product.discount",
          "discountFromDate" -> "$product.discountFromDate",
          "discountEndDate" -> "$product.discountEndDate",
          "images" -> "$product.productImage",
          "createDate" -> "$product.createDate",
          "views" -> "$views",
          "categoryName" -> "$categoryName",
          "subCategoryName" -> ""
        )
      ),
      "total" -> SumAll
    )
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "product"
      ),
      List(
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Match(Json.obj("productType" -> productType, "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")), "status" -> 1)),
        Sort(Descending("product.createDate")),
        group,
        Skip((start - 1) * limit),
        Limit(limit),
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        )
      )
    ).map(_.head[JsObject]))

    if (productType != "normal") {
      products = categoryCollection.flatMap(_.aggregate(
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "product"
        ),
        List(
          UnwindField("product"),
          Lookup(
            "product_view_tbl",
            "product._id",
            "productId",
            "view"
          ),
          UnwindField("view"),
          Lookup(
            "store_tbl",
            "product._id",
            "productId",
            "store"
          ),
          UnwindField("store"),
          Lookup(
            "user_tbl",
            "store.userId",
            "_id",
            "user"
          ),
          UnwindField("user"),
          project,
          Match(Json.obj("productType" -> productType, "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")), "status" -> 1)),
          group,
          Sample(Int.MaxValue),
          Skip((start - 1) * limit),
          Limit(limit),
          UnwindField("product"),
          Project(
            Json.obj(
              "product" -> 1,
              "total" -> 1
            )
          )
        )
      ).map(_.head[JsObject]))
    }

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products by name with result : " + data)
    }
    products
  }

  override def getProductByMultiFilterWithLocation(productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      /** Date Rang == 0, 1, 2, 3, 4 (Any, Today, This Week, This Month, This Year) **/
      val today = new DateTime()
      var startDate: Long = 0
      var endDate: Long = 0
      if (dateRang == 1) {
        startDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
        endDate = FormatDate.parseDate(FormatDate.printDate(today)).getMillis
      } else if (dateRang == 2) {
        startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMinimumValue())).getMillis
        endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfWeek().withMaximumValue())).getMillis
      } else if (dateRang == 3) {
        startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMinimumValue())).getMillis
        endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfMonth().withMaximumValue())).getMillis
      } else if (dateRang == 4) {
        startDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMinimumValue())).getMillis
        endDate = FormatDate.parseDate(FormatDate.printDate(today.dayOfYear().withMaximumValue())).getMillis
      } else {
        println("No date rang")
        startDate = 0
        endDate = 0
      }
      /*===============Location=====================*/
      if (productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and Location")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and date rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name and price rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name, date rang and price rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name , location and date rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name , location and price rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name , location, date rang and price rang")
        Json.obj(
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*===============Location=====================*/ else if (productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Location")
        Json.obj(
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Location and date rang")
        Json.obj(
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location and price rang")
        Json.obj(
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location, date rang and price rang")
        Json.obj(
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*===============Date rang=====================*/ else if (productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Date rang")
        Json.obj(
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Date rang and price rang")
        Json.obj(
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } /*===============Price rang=====================*/ else if (productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Price")
        Json.obj(
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else {
        println("No sub category => DEFAULT")
        Json.obj(
          "productType" -> productType,
          "status" -> 1
        )
      }
    }

    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> ""
      )
    )
    /** Group **/
    val group: Group = Group(JsString("$state"))(
      "product" -> Push(
        Json.obj(
          "id" -> "$id",
          "name" -> "$name",
          "description" -> "$description",
          "location" -> "$location",
          "price" -> "$price",
          "discount" -> "$discount",
          "discountFromDate" -> "$discountFromDate",
          "discountEndDate" -> "$discountEndDate",
          "images" -> "$images",
          "createDate" -> "$createDate",
          "views" -> "$views",
          "categoryName" -> "$categoryName",
          "subCategoryName" -> ""
        )
      ),
      "total" -> SumAll
    )

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "product_tbl",
        "productId",
        "_id",
        "product"
      ),
      List(
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Match(filter),
        Sort(Descending("createDate")),
        group,
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((page - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    /** productType == hot or gold **/
    if (productType != "normal") {
      products = categoryCollection.flatMap(_.aggregate(
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "product"
        ),
        List(
          UnwindField("product"),
          Lookup(
            "product_view_tbl",
            "product._id",
            "productId",
            "view"
          ),
          UnwindField("view"),
          Lookup(
            "store_tbl",
            "product._id",
            "productId",
            "store"
          ),
          UnwindField("store"),
          Lookup(
            "user_tbl",
            "store.userId",
            "_id",
            "user"
          ),
          UnwindField("user"),
          project,
          Match(filter),
          Sample(Int.MaxValue),
          group,
          UnwindField("product"),
          Project(
            Json.obj(
              "product" -> 1,
              "total" -> 1
            )
          ),
          Skip((page - 1) * limit),
          Limit(limit)
        )
      ).map(_.head[JsObject]))
    }

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products multi filter with result : " + data)
    }
    products
  }

  override def getSubCategoryRepo(categoryName: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val category = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "secondCategory"
      ),
      List(
        UnwindField("secondCategory"),
        Match(Json.obj("categoryName" -> categoryName))
      )
    ).map(_.head[JsObject]))
    category.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all second category name ", categoryName)
    }
    category
  }

  override def getProductRelatedRepo(checkCategory: Int, categoryName: String, productId: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    def filter: JsObject = {
      if (checkCategory > 0) {
        Json.obj(
          "categoryName" -> categoryName,
          "id" -> Json.obj("$ne" -> BSONObjectID.parse(productId).get),
          "status" -> 1
        )
      } else {
        Json.obj(
          "subCategoryName" -> categoryName,
          "id" -> Json.obj("$ne" -> BSONObjectID.parse(productId).get),
          "status" -> 1
        )
      }
    }
    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    if (checkCategory > 0) {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> "$subCategoryName"
          )
        ),
        "total" -> SumAll
      )
    } else {
      group = Group(JsString("$state"))(
        "product" -> Push(
          Json.obj(
            "id" -> "$id",
            "name" -> "$name",
            "description" -> "$description",
            "location" -> "$location",
            "price" -> "$price",
            "discount" -> "$discount",
            "discountFromDate" -> "$discountFromDate",
            "discountEndDate" -> "$discountEndDate",
            "images" -> "$images",
            "createDate" -> "$createDate",
            "views" -> "$views",
            "categoryName" -> "$categoryName",
            "subCategoryName" -> ""
          )
        ),
        "total" -> SumAll
      )
    }

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Sort(Descending("productType")),
        Sort(Descending("createDate")),
        Match(filter),
        group,
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products multiple filter with location !!")
    }
    products
  }

  override def getProductRecentlyRepo(start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    group = Group(JsString("$state"))(
      "product" -> Push(
        Json.obj(
          "id" -> "$id",
          "name" -> "$name",
          "description" -> "$description",
          "location" -> "$location",
          "price" -> "$price",
          "discount" -> "$discount",
          "discountFromDate" -> "$discountFromDate",
          "discountEndDate" -> "$discountEndDate",
          "images" -> "$images",
          "createDate" -> "$createDate",
          "views" -> "$views",
          "categoryName" -> "$categoryName",
          "subCategoryName" -> "$subCategoryName"
        )
      ),
      "total" -> SumAll
    )
    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Sort(Descending("productType")),
        Sort(Descending("createDate")),
        group,
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products multiple filter with result : " + data)
    }
    products
  }

  override def getProductByUserNameRepo(username: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    /** Project **/
    val project = Project(
      Json.obj(
        "id" -> "$product._id",
        "productType" -> "$product.productType",
        "name" -> "$product.productName",
        "productName" -> Json.obj("$toLower" -> "$product.productName"),
        "description" -> "$product.productDescription",
        "location" -> "$user.city",
        "username" -> "$user.userName",
        "price" -> "$product.price",
        "discount" -> "$product.discount",
        "discountFromDate" -> "$product.discountFromDate",
        "discountEndDate" -> "$product.discountEndDate",
        "images" -> "$product.productImage",
        "createDate" -> "$product.createDate",
        "status" -> "$product.status",
        "views" -> Json.obj("$sum" -> "$view.viewer.views"),
        "categoryName" -> 1,
        "subCategoryName" -> "$sub.categoryName"
      )
    )
    /** Group **/
    var group: Group = null
    group = Group(JsString("$state"))(
      "product" -> Push(
        Json.obj(
          "id" -> "$id",
          "name" -> "$name",
          "description" -> "$description",
          "location" -> "$location",
          "username" -> "$username",
          "price" -> "$price",
          "discount" -> "$discount",
          "discountFromDate" -> "$discountFromDate",
          "discountEndDate" -> "$discountEndDate",
          "images" -> "$images",
          "createDate" -> "$createDate",
          "views" -> "$views",
          "categoryName" -> "$categoryName",
          "subCategoryName" -> "$subCategoryName"
        )
      ),
      "total" -> SumAll
    )

    /** productType == normal **/
    var products = categoryCollection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "product_tbl",
          "sub.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "store_tbl",
          "product._id",
          "productId",
          "store"
        ),
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        UnwindField("user"),
        project,
        Sort(Descending("productType")),
        Sort(Descending("createDate")),
        Match(Json.obj("username" -> username, "status" -> 1)),
        group,
        UnwindField("product"),
        Project(
          Json.obj(
            "product" -> 1,
            "total" -> 1
          )
        ),
        Skip((start - 1) * limit),
        Limit(limit)
      )
    ).map(_.head[JsObject]))

    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get products by username : " + data)
    }
    products
  }

  /**=============================================MANAGE PRODUCT=============================================**/

  override def getProductByUserId(userId: String, start: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val products = userCollection.flatMap(_.aggregate(
      Lookup(
        "store_tbl",
        "_id",
        "userId",
        "store"
      ),
      List(
        UnwindField("store"),
        Lookup(
          "product_tbl",
          "store.productId",
          "_id",
          "product"
        ),
        UnwindField("product"),
        Lookup(
          "product_view_tbl",
          "product._id",
          "productId",
          "view"
        ),
        UnwindField("view"),
        Lookup(
          "promoted_product_tbl",
          "product._id",
          "productId",
          "promote"
        ),
        Match(Json.obj("_id" -> BSONObjectID.parse(userId).get, "product.status" -> 1)),
        Skip((start - 1) * limit),
        Limit(limit),
        Sort(Descending("product.createDate")),
        Project(
          Json.obj(
            "_id" -> 0,
            "id" -> "$product._id",
            "name" -> "$product.productName",
            "description" -> "$product.productDescription",
            "price" -> "$product.price",
            "type" -> "$product.productType",
            "discount" -> "$product.discount",
            "discountFromDate" -> "$product.discountFromDate",
            "discountEndDate" -> "$product.discountEndDate",
            "images" -> "$product.productImage",
            "createDate" -> "$product.createDate",
            "store" -> "$store.productId",
            "view" -> Json.obj("$sum" -> "$view.viewer.views"),
            "promote" -> 1
          )
        )
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get products by userId with result $data")
    }
    products
  }

  override def updateProductStatus(productId: String, status: Int): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> status
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update product with result $data")
    }
    writeRes
  }

  override def getProductByProductId(productId: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val products = productCollection.flatMap(_.aggregate(
      Lookup(
        "store_tbl",
        "_id",
        "productId",
        "store"
      ),
      List(
        UnwindField("store"),
        Lookup(
          "user_tbl",
          "store.userId",
          "_id",
          "user"
        ),
        Lookup(
          "category_tbl",
          "_id",
          "productId",
          "category"
        ),
        UnwindField("category"),
        Lookup(
          "category_tbl",
          "category.parentId",
          "_id",
          "sub"
        ),
        UnwindField("sub"),
        Lookup(
          "category_tbl",
          "sub.parentId",
          "_id",
          "main"
        ),
        Match(Json.obj("_id" -> BSONObjectID.parse(productId).get, "status" -> 1)),
        Project(
          Json.obj(
            "productName" -> 1,
            "productDescription" -> 1,
            "price" -> 1,
            "discount" -> 1,
            "discountFromDate" -> 1,
            "discountEndDate" -> 1,
            "productImage" -> 1,
            "category" -> Json.obj("categoryId" -> "$category._id", "categoryName" -> "$category.categoryName"),
            "subCategory" -> Json.obj("categoryId" -> "$sub._id", "categoryName" -> "$sub.categoryName"),
            "mainCategory" -> Json.obj("categoryId" -> "$main._id", "categoryName" -> "$main.categoryName")
          )
        )
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get products by id with result $data")
    }
    products
  }

  override def updateProductInfo(product: Product): Future[WriteResult] = {
    def filter: BSONDocument = {
      if (product.discountFromDate.get.getMillis > 0 && product.discountEndDate.get.getMillis > 0) {
        BSONDocument(
          "$set" -> BSONDocument(
            "productName" -> product.productName,
            "productDescription" -> product.productDescription,
            "price" -> product.price,
            "discount" -> product.discount,
            "discountFromDate" -> product.discountFromDate.get.getMillis,
            "discountEndDate" -> product.discountEndDate.get.getMillis
          )
        )
      } else {
        BSONDocument(
          "$set" -> BSONDocument(
            "productName" -> product.productName,
            "productDescription" -> product.productDescription,
            "price" -> product.price,
            "discount" -> product.discount
          )
        )
      }
    }
    val writeRes = productCollection.flatMap(_.update(Json.obj("_id" -> product._id), filter))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update product with $data")
    }
    writeRes
  }

  /* naseat */
  override def searchProductByName(checkCategory: Int, categoryName: String, productName: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    var categories = productCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 0,
          "productName" -> 1,
          "name" -> Json.obj("$toLower" -> "$productName")
        )
      ),
      List(
        Match(Json.obj(
          "name" -> Json.obj("$regex" -> ("^" + productName.toLowerCase + ".*"))
        )),
        Limit(10),
        Project(
          Json.obj(
            "productName" -> 1
          )
        )
      )
    ).map(_.head[JsObject]))

    if (categoryName != "") {
      if (checkCategory > 0) {
        println("I am here have sub ..." + categoryName)
        categories = categoryCollection.flatMap(_.aggregate(
          Lookup(
            "category_tbl",
            "_id",
            "parentId",
            "sub"
          ),
          List(
            UnwindField("sub"),
            Lookup(
              "product_tbl",
              "sub.productId",
              "_id",
              "product"
            ),
            UnwindField("product"),
            Project(
              Json.obj(
                "_id" -> 0,
                "product.productName" -> 1,
                "name" -> Json.obj("$toLower" -> "$product.productName"),
                "categoryName" -> 1
              )
            ),
            Match(Json.obj(
              "categoryName" -> categoryName,
              "name" -> Json.obj("$regex" -> ("^" + productName.toLowerCase + ".*"))
            )),
            Limit(10),
            Project(
              Json.obj(
                "productName" -> "$product.productName"
              )
            )
          )
        ).map(_.head[JsObject]))
      } else {
        println("I am here no sub ..." + categoryName)
        categories = productCollection.flatMap(_.aggregate(
          Lookup(
            "category_tbl",
            "_id",
            "productId",
            "category"
          ),
          List(
            UnwindField("category"),
            Project(
              Json.obj(
                "_id" -> 0,
                "productName" -> 1,
                "name" -> Json.obj("$toLower" -> "$productName"),
                "categoryName" -> "$category.categoryName"
              )
            ),
            Match(Json.obj(
              "categoryName" -> categoryName,
              "name" -> Json.obj("$regex" -> ("^" + productName.toLowerCase + ".*"))
            )),
            Limit(10),
            Project(
              Json.obj(
                "productName" -> 1
              )
            )
          )
        ).map(_.head[JsObject]))
      }
    }

    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful search productName" + data)
    }
    categories
  }

  def matchFilterReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, userName: String, userType: String): JsObject = {
    val fromDateLong = FormatDate.parseDate(dateStart).getMillis
    val toDateLong = FormatDate.parseDate(dateEnd).getMillis

    if ((status == 0 || status == 1 || status == -1) && location != "" && userName == "" && productType == "" && userType == "" && (fromDateLong < 0 && toDateLong < 0) && (startPrice < 0 || endPrice < 0)) {
      println("Status Location")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && location == "" && userName == "" && userType == ""
      && (fromDateLong < 0 && toDateLong < 0) && (startPrice < 0 || endPrice < 0)) {
      println("Status Type")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && productType == "" && userType == ""
      && location == "" && userName == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && location == "" && userName == "" && userType == "") {
      println("Status Price")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Name")
      Json.obj(
        "store_product.status" -> status,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && userName == "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType
      )
      //userName != "" and userType !="" other =""
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status UserName UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
      //userName != "" and userType !="" dateRange !=""
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status DateRank UserName UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
      //userName != "" and userType !="" dateRange !="" priceRank
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status priceRank DateRank UserName UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
      //userName != "" and userType !="" dateRange !="" priceRank location !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status priceRank DateRank UserName UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.city" -> location
      )
      //userName != "" and userType !="" dateRange !="" priceRank location !="" productType !=""
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status priceRank DateRank UserName UserType productType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.city" -> location,
        "store_product.productType" -> productType
      )
      //userName != "" and userType !="" dateRange !="" priceRank  > 0 , location =="" productType !=""
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status priceRank DateRank UserName UserType productType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_product.productType" -> productType
      )
      //userName != "" and userType !="" dateRange =="" priceRank < 0  location =="" productType !=""
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status   UserName UserType productType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType
      )
      //userName != "" and userType !=""    productType !="" location !="" dateRange =="" priceRank < 0
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status   UserName UserType productType location")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.city" -> location,
        "store_product.productType" -> productType
      )
      //userName != "" and userType !=""    productType !="" location !="" dateRange =="" priceRank > 0
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status   UserName UserType productType location price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
      //userName != "" and userType !=""    productType =="" location !="" dateRange =="" priceRank < 0
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status   UserName UserType  location dateRange")
      Json.obj(
        "store_product.status" -> status,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
      // location =="" , productType !="" , dateRange !="" , priceRank =="" , userType == "" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice < 0 || endPrice < 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status   UserName dateRange")
      Json.obj(
        "store_product.status" -> status,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.productType" -> productType
      )
      // location !="" , productType =="" , dateRange !="" , priceRank !="" , userType != "" , userName =="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName == "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status Location dateRange priceRank userType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.userType" -> userType,
        "store_user.city" -> location
      )
      // location !="" , productType !="" , dateRange !="" , priceRank !="" , userType =="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status dateRange location priceRank userName productType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType,
        "store_user.city" -> location
      )
      // location =="" , productType !="" , dateRange !="" , priceRank !="" , userType =="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status dateRange priceRank userName productType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType
      )
      // location =="" , productType =="" , dateRange =="" , priceRank !="" , userType !="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status  priceRank userName  userType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.userType" -> userType
      )

      // location =="" , productType !="" , dateRange !="" , priceRank !="" , userType !="" , userName =="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName == "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status productType priceRank dateRange   userType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_user.userType" -> userType,
        "store_product.productType" -> productType
      )
      // location !="" , productType !="" , dateRange =="" , priceRank =="" , userType =="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice < 0 || endPrice < 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status location productType userName")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType
      )
      // location =="" , productType !="" , dateRange =="" , priceRank !="" , userType !="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location == "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status productType priceRank  userType userName")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType,
        "store_user.userType" -> userType
      )
      // location !="" , productType !="" , dateRange =="" , priceRank !="" , userType =="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && productType != "") {
      println("Status location productType priceRank userName")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_product.productType" -> productType,
        "store_user.city" -> location
      )

      // location !="" , productType =="" , dateRange =="" , priceRank !="" , userType =="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType == "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType == "") {
      println("Status location priceRank dateRange userName")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
      // location !="" , productType =="" , dateRange =="" , priceRank =="" , userType !="" , userName !="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status location userType userName")
      Json.obj(
        "store_product.status" -> status,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.city" -> location,
        "store_user.userType" -> userType
      )
      // location !="" , productType !="" , dateRange !="" , priceRank !="" , userType !="" , userName =="" 
    } else if ((status == 0 || status == 1 || status == -1) && userName == "" && userType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong > 0 && toDateLong > 0) && productType != "") {
      println("Status location productType dateRange priceRank userType ")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_user.userType" -> userType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_product.productType" -> productType

      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && userName == "" && userType == "" && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Location Type")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && productType == "" && userName == "" && userType == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Location Date")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && userName == "" && userType == "") {
      println("Status Location Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && userName != "" && userType == ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Location Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && userName == "" && userType != ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && productType == "") {
      println("Status Location UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (fromDateLong > 0 && toDateLong > 0)
      && location == "" && userName == "" && userType == "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Type Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong < 0 && toDateLong < 0) && location == "" && userName == "" && userType == "") {
      println("Status Type Price")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && userName != "" && userType == ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && location == "") {
      println("Status Type Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && userName == "" && userType != ""
      && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0) && location == "") {
      println("Status Type userType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0)
      && productType == "" && userName == "" && userType == "" && location == "") {
      println("Status Date Price" + startPrice + "/" + endPrice)
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && userName != ""
      && (startPrice < 0 || endPrice < 0) && productType == "" && location == "" && userType == "") {
      println("Status Date Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && (fromDateLong > 0 && toDateLong > 0) && userName == ""
      && (startPrice < 0 || endPrice < 0) && productType == "" && location == "" && userType != "") {
      println("Status Date UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0) && userName != ""
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && location == "" && userType == "") {
      println("Status Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0) && userName == ""
      && (fromDateLong < 0 && toDateLong < 0) && productType == "" && location == "" && userType != "") {
      println("Status Price UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice < 0 || endPrice < 0) && userName == "" && userType == "") {
      println("Status Location Type Date")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && (startPrice > 0 || endPrice > 0) && productType == "" && userName == "" && userType == "") {
      println("Status Location Date Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && userName != "" && (startPrice < 0 || endPrice < 0) && productType == "" && userType == "") {
      println("Status Location Date Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (fromDateLong > 0 && toDateLong > 0)
      && userName == "" && (startPrice < 0 || endPrice < 0) && productType == "" && userType != "") {
      println("Status Location Date UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (startPrice > 0 || endPrice > 0)
      && userName != "" && (fromDateLong < 0 && toDateLong < 0) && productType == "" && userType == "") {
      println("Status Location Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && (startPrice > 0 || endPrice > 0)
      && userName == "" && (fromDateLong < 0 && toDateLong < 0) && productType == "" && userType != "") {
      println("Status Location Price UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (startPrice > 0 || endPrice > 0) && (fromDateLong < 0 && toDateLong < 0) && userName == "" && userType == "") {
      println("Status Location Type Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && userName == "" && userType == "" && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Location Type Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_user.userType" -> userType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && userName == "" && userType != "" && (startPrice < 0 || endPrice < 0) && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Location Type userType Name")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && userName != "" && userType == "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Type Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && userName == "" && userType != "" && location == "" && (fromDateLong < 0 && toDateLong < 0)) {
      println("Status Type Price UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && (fromDateLong > 0 && toDateLong > 0) && userName == "" && location == "" && userType == "") {
      println("Status Type Price Date")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong)
      )
    } else if ((status == 0 || status == 1 || status == -1) && productType != "" && (startPrice > 0 || endPrice > 0)
      && location != "" && (fromDateLong < 0 && toDateLong < 0) && userName == "" && userType == "") {
      println("Status Type Price Location")
      Json.obj(
        "store_product.status" -> status,
        "store_product.productType" -> productType,
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.city" -> location
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0)
      && (fromDateLong > 0 && toDateLong > 0) && userName != "" && userType == "" && location == "" && productType == "") {
      println("Status Date Price Name")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*"))
      )
    } else if ((status == 0 || status == 1 || status == -1) && (startPrice > 0 || endPrice > 0)
      && (fromDateLong > 0 && toDateLong > 0) && userName == "" && userType != "" && location == "" && productType == "") {
      println("Status Date Price UserType")
      Json.obj(
        "store_product.status" -> status,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0) && userName == "" && userType == "") {
      println("Status Location Type Date Price")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice)
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && userName != "" && userType != "" && (startPrice < 0 || endPrice < 0)) {
      println("Status Location Type Date Name userType")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.userType" -> userType
      )
    } else if ((status == 0 || status == 1 || status == -1) && location != "" && productType != ""
      && (fromDateLong > 0 && toDateLong > 0) && (startPrice > 0 || endPrice > 0) && userName != "" && userType != "") {
      println("All")
      Json.obj(
        "store_product.status" -> status,
        "store_user.city" -> location,
        "store_product.productType" -> productType,
        "store_product.createDate" -> Json.obj("$gte" -> fromDateLong, "$lte" -> toDateLong),
        "store_product.price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "lowName" -> Json.obj("$regex" -> ("^" + userName.toLowerCase + ".*")),
        "store_user.userType" -> userType
      )
    } else {
      println("Default is Status")
      Json.obj(
        "store_product.status" -> status
      )
    }
  }

  /* Helper Function Report*/
  def getCountProductsReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, userName: String, userType: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = storeCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "store_user"
      ),
      List(
        UnwindField("store_user"),
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "store_product"
        ),
        UnwindField("store_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$store_user.userName"),
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user.city" -> 1,
            "store_user.userName" -> 1,
            "store_user.userType" -> 1
          )
        ),
        Match(matchFilterReport(location, productType, status, dateStart, dateEnd, startPrice, endPrice, userName, userType)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get count")
    }
    total
  }

  override def listProductsReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, userName: String, userType: String, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val jsObj: List[JsObject] = Await.result(getCountProductsReport(location, productType, status, dateStart, dateEnd, startPrice, endPrice, userName, userType), 10.seconds)
    var total: Int = 0
    if (jsObj.nonEmpty) {
      total = (jsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("Total Product Report Count" + total)
    val products = storeCollection.flatMap(_.aggregate(
      Lookup(
        "user_tbl",
        "userId",
        "_id",
        "store_user"
      ),
      List(
        UnwindField("store_user"),
        Lookup(
          "product_tbl",
          "productId",
          "_id",
          "store_product"
        ),
        UnwindField("store_product"),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "lowName" -> Json.obj("$toLower" -> "$store_user.userName"),
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user.city" -> 1,
            "store_user.userName" -> 1,
            "store_user.userType" -> 1
          )
        ),
        Match(matchFilterReport(location, productType, status, dateStart, dateEnd, startPrice, endPrice, userName, userType)),
        Sort(Ascending("lowName")),
        Skip((page * limit) - limit),
        Limit(limit),
        Project(
          Json.obj(
            "_id" -> 1,
            "store_product._id" -> 1,
            "store_product.productName" -> 1,
            "store_product.productDescription" -> 1,
            "store_product.productImage" -> 1,
            "store_product.price" -> 1,
            "store_product.discount" -> 1,
            "store_product.discountFromDate" -> 1,
            "store_product.discountEndDate" -> 1,
            "store_product.productType" -> 1,
            "store_product.createDate" -> 1,
            "store_product.expireDate" -> 1,
            "store_product.status" -> 1,
            "store_product.blockDate" -> 1,
            "storeName" -> 1,
            "store_user.city" -> 1,
            "store_user.userName" -> 1,
            "store_user.userType" -> 1,
            "total" -> s"$total"
          )
        )
      )
    ).map(_.head[JsObject]))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all products Report")
    }
    products
  }

  override def updateProductStatusById(id: String, status: Int): Future[WriteResult] = {
    val writeRes = productCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> status,
          "blockDate" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())).getMillis
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful updated product's status !!")
    }
    writeRes
  }

  /* sopheak */
  override def countTodayProducts(): Future[Int] = {
    val products = productCollection.flatMap(_.find(Json.obj("createDate" -> Json.obj("$gte" -> FormatDate.parseDate(FormatDate.printDate(new DateTime())))))
      .cursor[Product](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Product]]()))
    products.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get product count !!")
    }
    products.map(product => product.length)
  }

}

