package utils

import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._

object FilterProducts {

  def filterProductWithCategory(check: Int, categoryId: String, subId: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double): JsObject = {
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

    if (check > 0) {
      // Have sub category
      /* ================ sub pro ==========*/
      if (subId != "" && productName == "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub Category : " + subId)
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "subId" -> BSONObjectID.parse(subId).get,
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category and product name")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name and location")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, product name, location and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, location and price rang")
        Json.obj(
          "id" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, product name, location, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        /* ================ sub location ==========*/
        println("Sub category and location")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Sub category, location and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, location and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, location, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        /*=============Sub date rang=================*/
        println("Sub category and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId != "" && productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Sub category and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "subId" -> BSONObjectID.parse(subId).get,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        /*=============Name=================*/
        println("Product name")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("Product name and location")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Product name and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Product name, location and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, location and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Product name, location, date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        /*=============Location=================*/
        println("Location")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("Location and date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("Location and price")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Location, date rang and price")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        /*=============Date rang=================*/
        println("Date rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("Date rang and price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (subId == "" && productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        /*=============Price rang=================*/
        println("Price rang")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else {
        println("DEFAULT")
        Json.obj(
          "categoryId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "status" -> 1
        )
      }
    } else {
      // No sub category
      if (productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and Location")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Product name and date rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => ProductName , location and date rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => ProductName , location and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Product name, date rang and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => ProductName , location, date rang and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
        /*=================Location======================*/
        println("No sub category => Location")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        println("No sub category => Location and date rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Location, date rang and price rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "location" -> location,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
        /*=================Date rang======================*/
        println("No sub category => Date rang")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "status" -> 1
        )
      } else if (productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
        println("No sub category => Date rang and price")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else if (productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
        /*=================Price rang======================*/
        println("No sub category => Price rang") // Filter No sub category => Price
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
          "status" -> 1
        )
      } else {
        println("No sub category => DEFAULT")
        Json.obj(
          "subId" -> BSONObjectID.parse(categoryId).get,
          "productType" -> productType,
          "status" -> 1
        )
      }
    }
  }

  def filterProductWithLocation(productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double): JsObject = {
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
    if (productName != "" && location == "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
      println("Product name")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "status" -> 1
      )
    } else if (productName != "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
      println("Product name and Location")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "location" -> location,
        "status" -> 1
      )
    } else if (productName != "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
      println("Product name and date rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "status" -> 1
      )
    } else if (productName != "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
      println("Product name and price rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName != "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
      println("Product name , location and date rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "location" -> location,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "status" -> 1
      )
    } else if (productName != "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
      println("ProductName , location and price rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "location" -> location,
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName != "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
      println("Product name, date rang and price rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName != "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
      println("ProductName , location, date rang and price rang")
      Json.obj(
        "productType" -> productType,
        "productName" -> Json.obj("$regex" -> (".*" + productName.toLowerCase + ".*")),
        "location" -> location,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName == "" && location != "" && dateRang == 0 && startPrice == 0 && endPrice == 0) {
      /*=================Location======================*/
      println("No sub category => Location")
      Json.obj(
        "productType" -> productType,
        "location" -> location,
        "status" -> 1
      )
    } else if (productName == "" && location != "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
      println("Location and date rang")
      Json.obj(
        "productType" -> productType,
        "location" -> location,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "status" -> 1
      )
    } else if (productName == "" && location != "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
      println("Location and price rang")
      Json.obj(
        "productType" -> productType,
        "location" -> location,
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName == "" && location != "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
      println("Location, date rang and price rang")
      Json.obj(
        "productType" -> productType,
        "location" -> location,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName == "" && location == "" && dateRang > 0 && startPrice == 0 && endPrice == 0) {
      /*=================Date rang======================*/
      println("Date rang")
      Json.obj(
        "productType" -> productType,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "status" -> 1
      )
    } else if (productName == "" && location == "" && dateRang > 0 && startPrice >= 0 && endPrice > 0) {
      println("Date rang and price")
      Json.obj(
        "productType" -> productType,
        "createDate" -> Json.obj("$gte" -> startDate, "$lte" -> endDate),
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else if (productName == "" && location == "" && dateRang == 0 && startPrice >= 0 && endPrice > 0) {
      /*=================Price rang======================*/
      println("Price rang")
      Json.obj(
        "productType" -> productType,
        "price" -> Json.obj("$gte" -> startPrice, "$lte" -> endPrice),
        "status" -> 1
      )
    } else {
      println("DEFAULT")
      Json.obj(
        "productType" -> productType,
        "status" -> 1
      )
    }
  }

}
