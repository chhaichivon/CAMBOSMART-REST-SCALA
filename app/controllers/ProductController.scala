package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import services.product.IProductService
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.category.ICategoryService

import scala.concurrent.ExecutionContext.Implicits.global

class ProductController @Inject() (val iProductService: IProductService, val categoryService: ICategoryService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /*chivon*/
  /**List Product By Category Name**/
  def listProductByCategoryName(categoryName: String, start: Int, limit: Int) = ApiAction { implicit request =>
    categoryService.checkSubCategory(categoryName).flatMap {
      case categories if categories.nonEmpty =>
        iProductService.getProductByCategoryName(1, categoryName, start, limit).flatMap(
          products => ok(Json.obj(
            "message" -> "Products found",
            "code" -> 200,
            "products" -> products
          ))
        ).fallbackTo(errorInternal)

      case categories if categories.isEmpty =>
        iProductService.getProductByCategoryName(0, categoryName, start, limit).flatMap(
          products => ok(Json.obj(
            "message" -> "Products found",
            "code" -> 200,
            "products" -> products
          ))
        ).fallbackTo(errorInternal)
    }
  }

  /**Filter products**/
  implicit val listProductFilterReads: Reads[(String, String, String, String, String, Int, Double, Double)] =
    (__ \ "categoryName").read[String] and
      (__ \ "subCategoryName").read[String] and
      (__ \ "productType").read[String] and
      (__ \ "productName").read[String] and
      (__ \ "location").read[String] and
      (__ \ "dateRang").read[Int] and
      (__ \ "startPrice").read[Double] and
      (__ \ "endPrice").read[Double] tupled

  def listProductMultiFilter(page: Int, limit: Int) = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String, String, Int, Double, Double)] {
      case (categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice) =>
        if (categoryName != "") {
          categoryService.checkSubCategory(categoryName).flatMap {
            case categories if categories.nonEmpty =>
              iProductService.getProductByMultiFilterWithCategory(1, categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice, page, limit).flatMap(
                products => ok(Json.obj(
                  "message" -> "Products found",
                  "code" -> 200,
                  "products" -> products
                ))
              ).fallbackTo(errorInternal)
            case categories if categories.isEmpty =>
              iProductService.getProductByMultiFilterWithCategory(0, categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice, page, limit).flatMap(
                products => ok(Json.obj(
                  "message" -> "Products found",
                  "code" -> 200,
                  "products" -> products
                ))
              ).fallbackTo(errorInternal)
          }
        } else {
          iProductService.getProductByMultiFilterWithLocation(productType, productName, location, dateRang, startPrice, endPrice, page, limit).flatMap(
            products => ok(Json.obj(
              "message" -> "Products found",
              "code" -> 200,
              "products" -> products
            ))
          ).fallbackTo(errorInternal)
        }
    }
  }

  /**List Product Related**/
  def listProductRelated(categoryName: String, productId: String, start: Int, limit: Int) = ApiAction { implicit request =>
    categoryService.checkSubCategory(categoryName).flatMap {
      case categories if categories.nonEmpty =>
        iProductService.getProductRelatedService(1, categoryName, productId, start, limit).flatMap(
          products => ok(Json.obj(
            "message" -> "Products found",
            "code" -> 200,
            "products" -> products
          ))
        ).fallbackTo(errorInternal)

      case categories if categories.isEmpty =>
        iProductService.getProductRelatedService(0, categoryName, productId, start, limit).flatMap(
          products => ok(Json.obj(
            "message" -> "Products found",
            "code" -> 200,
            "products" -> products
          ))
        ).fallbackTo(errorInternal)
    }
  }

  /**List Product Recently**/
  def listProductRecently(start: Int, limit: Int) = ApiAction { implicit request =>
    iProductService.getProductRecentlyService(start, limit).flatMap(
      products => ok(Json.obj(
        "message" -> "Products found",
        "code" -> 200,
        "products" -> products
      ))
    ).fallbackTo(errorInternal)
  }
  /**LIST PRODUCT BY USER NAME**/
  def listProductByUserName(userName: String, start: Int, limit: Int) = ApiAction { implicit request =>
    iProductService.getProductByUserNameService(userName, start, limit).flatMap(
      products => ok(Json.obj(
        "message" -> "Products found",
        "code" -> 200,
        "products" -> products
      ))
    ).fallbackTo(errorInternal)
  }

  /** ==============================================================Admin======================================================**/

  implicit val filterProductsReads: Reads[(String, String, Int, String, String, Double, Double, String)] =
    (__ \ "storeLocation").read[String] and
      (__ \ "productType").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] and
      (__ \ "startPrice").read[Double] and
      (__ \ "endPrice").read[Double] and
      (__ \ "name").read[String] tupled

  /* ADMIN FILTER PRODUCTS */
  def filterProducts(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, String, String, Double, Double, String)] {
      case (storeLocation, productType, status, fromDate, toDate, startPrice, endPrice, name) => {
        iProductService.listProducts(storeLocation, productType, status, fromDate, toDate, startPrice, endPrice, name, page, limit).flatMap {
          case products if products.nonEmpty =>
            ok(Json.obj(
              "message" -> "Products found",
              "code" -> 200,
              "products" -> products
            ))
          case products if products.isEmpty =>
            ok(Json.obj(
              "message" -> "Products not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }
  implicit val filterProductsReport: Reads[(String, String, Int, String, String, String, String, Double, Double)] =
    (__ \ "storeLocation").read[String] and
      (__ \ "productType").read[String] and
      (__ \ "status").read[Int] and
      (__ \ "fromDate").read[String] and
      (__ \ "toDate").read[String] and
      (__ \ "userName").read[String] and
      (__ \ "userType").read[String] and
      (__ \ "startPrice").read[Double] and
      (__ \ "endPrice").read[Double] tupled

  /* ADMIN LIST PRODUCT REPORT */
  def listProductsReport(page: Int, limit: Int) = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, Int, String, String, String, String, Double, Double)] {
      case (storeLocation, productType, status, fromDate, toDate, startPrice, endPrice, userName, userType) => {
        iProductService.listProductsReport(storeLocation, productType, status, fromDate, toDate, startPrice, endPrice, userName, userType, page, limit).flatMap {
          case products if products.nonEmpty =>
            ok(Json.obj(
              "message" -> "Products found",
              "code" -> 200,
              "products" -> products
            ))
          case products if products.isEmpty =>
            ok(Json.obj(
              "message" -> "Products not found",
              "code" -> 500
            ))
        }.fallbackTo(errorInternal)
      }
    }
  }
  /* ADMIN GET PRODUCT INFORMATION */
  def getProductInfoById(id: String) = ApiAction { implicit request =>
    iProductService.getProductInfoById(id).flatMap {
      case products if products.nonEmpty =>
        ok(Json.obj(
          "message" -> "Products information found",
          "code" -> 200,
          "products" -> products
        ))
      case products if products.isEmpty =>
        ok(Json.obj(
          "message" -> "Products information not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

  /* ADMIN UPDATES PRODUCT'S STATUS */
  def updateProductStatusById(id: String, status: Int) = SecuredApiAction { implicit request =>
    iProductService.updateStatusProductById(id, status).flatMap {
      case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Product status has been updated", "code" -> 200))
      case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with updated product status", "code" -> 500))
    }.fallbackTo(errorInternal)
  }
  /* naseat update product status 1 , -1 */
  def updateStatusProduct(id: String) = SecuredApiAction { implicit request =>
    iProductService.getProductById(id).flatMap {
      case None => ok(Json.obj("message" -> "Product not found !!", "code" -> ERROR_NOTFOUND))
      case Some(product) =>
        if (product.status > 0) {
          iProductService.updateProductStatusById(id, -1).flatMap {
            case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Product status has been updated", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with updated product status", "code" -> ERROR_BADREQUEST))
          }.fallbackTo(errorInternal)
        } else if (product.status < 0) {
          iProductService.updateProductStatusById(id, 1).flatMap {
            case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Product status has been updated", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with updated product status", "code" -> ERROR_BADREQUEST))
          }.fallbackTo(errorInternal)
        } else {
          ok(Json.obj("message" -> "Product inactive !!", "code" -> ERROR_USER_DISABLE))
        }
    }
  }
  /* ADMIN DELETE PRODUCT */
  def deleteProductStatusById(id: String) = SecuredApiAction { implicit request =>
    iProductService.deleteProductById(id).flatMap {
      case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Product status has been deleted", "code" -> 200))
      case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with delete product status", "code" -> 500))
    }.fallbackTo(errorInternal)
  }

  def listRelatedProducts(cat_id: String, pro_id: String) = ApiAction { implicit request =>
    iProductService.listRelatedProducts(cat_id, pro_id).flatMap(products => ok(products))
  }

  def listRecentlyProducts() = ApiAction { implicit request =>
    iProductService.listRecentlyProducts().flatMap(products => ok(products))
  }

  /** oudam **/

  implicit val productCategoryReads: Reads[(String, String, String)] =
    (__ \ "categoryName").read[String] and
      (__ \ "productType").read[String] and
      (__ \ "productName").read[String] tupled

  def listProductByNameAndCategoryName(start: Int, limit: Int) = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String)] {
      case (categoryName, productType, productName) =>
        if (categoryName != "") {
          categoryService.checkSubCategory(categoryName).flatMap {
            case categories if categories.nonEmpty =>
              iProductService.getProductsByNameAndCategoryName(1, categoryName, productType, productName, start, limit).flatMap(
                products => ok(Json.obj(
                  "message" -> "Products found !!",
                  "code" -> 200,
                  "products" -> products
                ))
              ).fallbackTo(errorInternal)
            case categories if categories.isEmpty =>
              iProductService.getProductsByNameAndCategoryName(0, categoryName, productType, productName, start, limit).flatMap(
                products => ok(Json.obj(
                  "message" -> "Products found !!",
                  "code" -> 200,
                  "products" -> products
                ))
              ).fallbackTo(errorInternal)
          }
        } else {
          iProductService.getProductsByName(productType, productName, start, limit).flatMap(
            products =>
              ok(Json.obj(
                "message" -> "Products found !!",
                "code" -> 200,
                "products" -> products
              ))
          ).fallbackTo(errorInternal)
        }
    }
  }

  implicit val productNameReads: Reads[(String, String)] =
    (__ \ "categoryName").read[String] and
      (__ \ "productName").read[String] tupled

  def searchProductByName() = ApiActionWithBody { implicit request =>
    readFromRequest[(String, String)] {
      case (categoryName, productName) =>
        categoryService.checkSubCategory(categoryName).flatMap {
          case categories if categories.nonEmpty =>
            iProductService.searchProductByName(1, categoryName, productName).flatMap {
              products =>
                ok(Json.obj(
                  "message" -> "Products found",
                  "code" -> 200,
                  "products" -> products
                ))
            }.fallbackTo(errorInternal)
          case categories if categories.isEmpty =>
            iProductService.searchProductByName(0, categoryName, productName).flatMap {
              products =>
                ok(Json.obj(
                  "message" -> "Products found",
                  "code" -> 200,
                  "products" -> products
                ))
            }.fallbackTo(errorInternal)
        }
    }
  }

  /* sopheak */
  def countTodayProducts() = SecuredApiAction { implicit request =>
    iProductService.countTodayProducts().flatMap(result =>
      ok(Json.obj(
        "message" -> "Products Counting found",
        "code" -> 200,
        "data" -> result
      )))
  }
}
