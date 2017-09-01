package services.product

import com.google.inject.{ Inject, ImplementedBy }
import models.category.Category
import play.api.libs.json.JsObject
import models.store.Store
import models.product.Product
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.product.{ IProductRepo, ProductRepo }
import scala.concurrent.Future

@ImplementedBy(classOf[ProductService])
trait IProductService {

  def insertProduct(product: Product): Future[WriteResult]
  def getProductById(productId: String): Future[Option[Product]]
  def uploadProductImage(productId: String, image: List[String]): Future[WriteResult]
  def removeProductImage(productId: String, image: String): Future[WriteResult]
  def deleteProductById(productId: String): Future[WriteResult]
  def renewProductById(productId: String): Future[WriteResult]
  def updateProductById(productId: String, productName: String, productDescription: String, price: Double): Future[WriteResult]

  /* sona */
  def listProducts(location: String, productType: String, status: Int, dateStart: String, dateEnd: String,
    startPrice: Double, endPrice: Double, name: String, page: Int, limit: Int): Future[List[JsObject]]
  def updateStatusProductById(id: String, status: Int): Future[WriteResult]
  def getProductInfoById(id: String): Future[List[JsObject]]
  def listPromoteProductsByUserId(userId: String): Future[List[JsObject]]
  def getPromotedProductByProductId(id: String): Future[Option[Product]]

  /*chivon*/
  def getProductByCategoryName(checkSub: Int, categoryName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductByMultiFilterWithCategory(checkCategory: Int, categoryName: String, subCategoryName: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getProductByLocationFilter(productType: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getProductsByNameAndCategoryName(checkCategory: Int, categoryName: String, productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductsByName(productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductByMultiFilterWithLocation(productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def getSubCategoryRepo(categoryName: String): Future[List[JsObject]]
  def getProductRelatedService(checkCategory: Int, categoryName: String, productId: String, start: Int, limit: Int): Future[List[JsObject]]
  def getProductRecentlyService(start: Int, limit: Int): Future[List[JsObject]]
  def getProductByUserNameService(username: String, start: Int, limit: Int): Future[List[JsObject]]

  def listRelatedProducts(cat_id: String, pro_id: String): Future[List[JsObject]]
  def listRecentlyProducts(): Future[List[JsObject]]

  /*oudam*/
  def getProductByUserId(userId: String, start: Int, limit: Int): Future[List[JsObject]]
  def updateProductStatus(productId: String, status: Int): Future[WriteResult]
  def getProductByProductId(productId: String): Future[List[JsObject]]
  def updateProductInfo(product: Product): Future[WriteResult]

  /*naseat*/
  def searchProductByName(checkCategory: Int, categoryName: String, productName: String): Future[List[JsObject]]
  def listProductsReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, userName: String, userType: String, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]]
  def updateProductStatusById(id: String, status: Int): Future[WriteResult]

  /* sopheak */
  def countTodayProducts(): Future[Int]
}
class ProductService @Inject() (val iProductRepo: IProductRepo) extends IProductService {

  override def insertProduct(product: Product): Future[WriteResult] = iProductRepo.insertProduct(product)

  override def getProductById(productId: String): Future[Option[Product]] = iProductRepo.getProductById(productId)

  override def uploadProductImage(productId: String, image: List[String]): Future[WriteResult] = iProductRepo.uploadProductImage(productId, image)

  override def removeProductImage(productId: String, image: String): Future[WriteResult] = iProductRepo.removeProductImage(productId, image)

  override def deleteProductById(productId: String): Future[WriteResult] = iProductRepo.deleteProductById(productId)

  override def renewProductById(productId: String): Future[WriteResult] = iProductRepo.renewProductById(productId)

  override def updateProductById(productId: String, productName: String, productDescription: String, price: Double): Future[WriteResult] = iProductRepo.updateProductById(productId, productName, productDescription, price)
  /* sona */
  override def listProducts(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, startPrice: Double, endPrice: Double, name: String, page: Int, limit: Int): Future[List[JsObject]] =
    iProductRepo.listProducts(location, productType, status, dateStart, dateEnd, startPrice, endPrice, name, page, limit)
  override def updateStatusProductById(id: String, status: Int): Future[WriteResult] = iProductRepo.updateStatusProductById(id, status)
  override def listPromoteProductsByUserId(userId: String): Future[List[JsObject]] = iProductRepo.listPromoteProductsByUserId(userId)
  override def getPromotedProductByProductId(id: String): Future[Option[Product]] = iProductRepo.getPromotedProductByProductId(id)

  /*chivon*/
  override def getProductByCategoryName(checkSub: Int, categoryName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductByCategoryName(checkSub, categoryName, start, limit)
  }
  override def getProductByMultiFilterWithCategory(checkCategory: Int, categoryName: String, subCategoryName: String, productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductByMultiFilterWithCategory(checkCategory, categoryName, subCategoryName, productType, productName, location, dateRang, startPrice, endPrice, page, limit)
  }
  override def getProductByLocationFilter(productType: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductByLocationFilter(productType, location, dateRang, startPrice, endPrice, page, limit)
  }
  override def getProductByMultiFilterWithLocation(productType: String, productName: String, location: String, dateRang: Int, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductByMultiFilterWithLocation(productType, productName, location, dateRang, startPrice, endPrice, page, limit)
  }
  override def getProductsByNameAndCategoryName(checkCategory: Int, categoryName: String, productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductsByNameAndCategoryName(checkCategory, categoryName, productType, productName, start, limit)
  }
  override def getProductsByName(productType: String, productName: String, start: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductsByName(productType, productName, start, limit)
  }
  override def getSubCategoryRepo(categoryName: String): Future[List[JsObject]] = { iProductRepo.getSubCategoryRepo(categoryName) }
  override def getProductRelatedService(checkCategory: Int, categoryName: String, productId: String, start: Int, limit: Int): Future[List[JsObject]] = {
    iProductRepo.getProductRelatedRepo(checkCategory, categoryName, productId, start, limit)
  }
  override def getProductRecentlyService(start: Int, limit: Int): Future[List[JsObject]] = { iProductRepo.getProductRecentlyRepo(start, limit) }
  override def getProductByUserNameService(username: String, start: Int, limit: Int): Future[List[JsObject]] = { iProductRepo.getProductByUserNameRepo(username, start, limit) }
  override def getProductInfoById(id: String): Future[List[JsObject]] = iProductRepo.getProductInfoById(id)

  override def listRelatedProducts(cat_id: String, pro_id: String): Future[List[JsObject]] = iProductRepo.listRelatedProducts(cat_id, pro_id)
  override def listRecentlyProducts(): Future[List[JsObject]] = iProductRepo.listRecentlyProducts()
  /*oudam*/
  override def getProductByUserId(userId: String, start: Int, limit: Int): Future[List[JsObject]] = iProductRepo.getProductByUserId(userId, start, limit)

  override def updateProductStatus(productId: String, status: Int): Future[WriteResult] = iProductRepo.updateProductStatus(productId, status)

  override def getProductByProductId(productId: String): Future[List[JsObject]] = iProductRepo.getProductByProductId(productId)

  override def updateProductInfo(product: Product): Future[WriteResult] = iProductRepo.updateProductInfo(product)

  /*naseat*/
  override def searchProductByName(checkCategory: Int, categoryName: String, productName: String): Future[List[JsObject]] = iProductRepo.searchProductByName(checkCategory, categoryName, productName)

  override def listProductsReport(location: String, productType: String, status: Int, dateStart: String, dateEnd: String, userName: String, userType: String, startPrice: Double, endPrice: Double, page: Int, limit: Int): Future[List[JsObject]] =
    iProductRepo.listProductsReport(location, productType, status, dateStart, dateEnd, userName, userType, startPrice, endPrice, page, limit)

  override def updateProductStatusById(id: String, status: Int): Future[WriteResult] = iProductRepo.updateProductStatusById(id, status)

  /* sopheak */
  override def countTodayProducts(): Future[Int] = iProductRepo.countTodayProducts()
}