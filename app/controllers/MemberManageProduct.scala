package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.product.Product
import models.view.{ ProductView, Viewer }
import org.joda.time.DateTime
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import services.product.ProductService
import utils.{ FormatDate, ImageBase64 }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import reactivemongo.play.json.BSONFormats._
import play.api.libs.functional.syntax._
import play.api.libs.mailer.{ Email, MailerClient }
import services.User.IUserService
import services.category.ICategoryService
import services.store.IStoreService
import services.subscribe.ISubscribeService
import services.view.IProductViewService
import services.mistake.IMistakeService
import models.mistake.Mistake
class MemberManageProduct @Inject() (
    val productService: ProductService,
    val storeService: IStoreService,
    val categoryService: ICategoryService,
    val userService: IUserService,
    val productViewService: IProductViewService,
    val subscribeService: ISubscribeService,
    mailer: MailerClient,
    val messagesApi: MessagesApi, system: ActorSystem
) extends api.ApiController {

  /** upload image */
  /*def uploadProductImage(productId: String) = SecuredApiActionWithBodyFile { request =>
    val images = request.body.files.toArray
    var imageNames: List[String] = List()
    images.foreach(file => {
      println("=====get file==========", file)
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val filename = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      file.ref.moveTo(new File(ImageBase64.productFilePath + filename))
      imageNames = imageNames :+ filename
    })
    productService.getProductById(productId).flatMap {
      case None => ok(Json.obj("message" -> "Product not found !!", "code" -> ERROR_PRODUCT_NOT_FOUND))
      case Some(product) =>
        if (product.productImage.get.nonEmpty) {
          product.productImage.get.foreach(
            image =>
              ImageBase64.removeFile(ImageBase64.productFilePath + image)
          )
          productService.uploadProductImage(productId, imageNames).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj("message" -> "Success save image 1", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image 1", "code" -> 500))
          }
        } else {
          productService.uploadProductImage(productId, imageNames).flatMap {
            case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Success save image 2 ", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail save image 2 ", "code" -> 500))
          }
        }
    }
  }*/

  implicit val productReads: Reads[(String, String, String, String, List[String], String, String, Product)] =
    (__ \ "userId").read[String] and
      (__ \ "ipAddress").read[String] and
      (__ \ "userType").read[String] and
      (__ \ "categoryId").read[String] and
      (__ \ "phones").read[List[String]] and
      (__ \ "location").read[String] and
      (__ \ "address").read[String] and
      (__ \ "product").read[Product] tupled
  def addNewProduct() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, String, String, List[String], String, String, Product)] {
      case (userId, ipAddress, userType, categoryId, phones, location, address, product) =>
        storeService.getStoreByUserId(userId).flatMap {
          case None =>
            ok(Json.obj(
              "message" -> "It can not get store according to the user id!",
              "code" -> ERROR_USER_ID
            ))
          case Some(store) =>
            if (store.productId.get.length >= 5 && userType == "normal") {
              ok(Json.obj(
                "message" -> "Normal user can only add 5 products!",
                "code" -> ERROR_PRODUCT_5
              ))
            } else if (store.productId.get.length >= 200 && userType == "merchant") {
              ok(Json.obj(
                "message" -> "Merchant user can only add 200 products!",
                "code" -> ERROR_PRODUCT_200
              ))
            } else {
              val id = BSONObjectID.generate()
              productService.insertProduct(
                Product(
                  Some(id),
                  product.productName,
                  product.productDescription,
                  Some(List()),
                  product.price,
                  product.discount,
                  product.discountFromDate,
                  product.discountEndDate,
                  Some("normal"),
                  Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
                  Some(FormatDate.parseDate("1970-01-01")),
                  product.status,
                  Some(FormatDate.parseDate("1970-01-01"))
                )
              ).flatMap {
                  case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail to insert product", "code" -> ERROR_BADREQUEST))
                  case writeRes if writeRes.n > 0 =>
                    //add view
                    productViewService.insertView(
                      ProductView(
                        Some(BSONObjectID.generate()),
                        Some(id),
                        List(Viewer(ipAddress, Some(new DateTime()), 1))
                      )
                    ).flatMap {
                        case wr if wr.n < 1 => ok(Json.obj("message" -> "Fail add product view !!", "code" -> ERROR_BADREQUEST))
                        case wr if wr.n > 0 => ok(Json.obj("message" -> "Successful add product view !!", "code" -> 200))
                      }
                    //update the productId in store table
                    var productId = store.productId.get
                    productId = productId :+ id
                    storeService.updateProductIdInStore(userId, productId).flatMap {
                      case wr if wr.n < 1 => ok(Json.obj("message" -> "Fail update productId in store !!", "code" -> ERROR_BADREQUEST))
                      case wr if wr.n > 0 => ok(Json.obj("message" -> "Successful update productId in store !!", "code" -> 200))
                    }
                    //update the productId in category table
                    categoryService.getCategoryById(categoryId).flatMap {
                      case None =>
                        ok(Json.obj(
                          "message" -> "It can not get category according to category id!",
                          "code" -> ERROR_NOTFOUND
                        ))
                      case Some(category) =>
                        var productId = category.productId
                        productId = productId :+ id
                        categoryService.updateProductIdInCategory(categoryId, productId).flatMap {
                          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update productId in category !!", "code" -> ERROR_BADREQUEST))
                          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update productId in category !!", "code" -> 200))
                        }.fallbackTo(errorInternal)
                    }
                    //update member information
                    userService.updateMemberProfile(userId, "", "", "", phones, location, address).flatMap {
                      case r if r.n < 1 => ok(Json.obj("message" -> "Fail update member information !!", "code" -> ERROR_BADREQUEST))
                      case r if r.n > 0 =>
                        ok(
                          Json.obj(
                            "message" -> "Successful update member information !!",
                            "productId" -> id.stringify,
                            "code" -> 200
                          )
                        )
                    }.fallbackTo(errorInternal)
                }
            }
        }
    }
  }
  def uploadImages(productId: String) = SecuredApiActionWithBodyFile { request =>
    val images = request.body.files.toArray
    var imageNames: List[String] = List()
    images.foreach(file => {
      println("=====get file==========", file)
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val filename = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      file.ref.moveTo(new File(ImageBase64.imagePath + filename))
      imageNames = imageNames :+ filename
    })
    productService.getProductById(productId).flatMap {
      case None => ok(Json.obj("message" -> "Product not found !!", "code" -> ERROR_NOTFOUND))
      case Some(product) =>
        if (product.productImage.get.nonEmpty) {
          product.productImage.get.foreach(
            image =>
              ImageBase64.removeFile(ImageBase64.imagePath + image)
          )
          productService.uploadProductImage(productId, imageNames).flatMap {
            case writeRes if writeRes.n > 0 =>
              ok(Json.obj("message" -> "Successful update images !!", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail update images !!", "code" -> ERROR_BADREQUEST))
          }
        } else {
          productService.uploadProductImage(productId, imageNames).flatMap {
            case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful update images !!", "code" -> 200))
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail update images !!", "code" -> ERROR_BADREQUEST))
          }
        }
    }
  }

  def listProductByUserId(userId: String, start: Int, limit: Int) = ApiAction { implicit request =>
    productService.getProductByUserId(userId, start, limit).flatMap(
      products => ok(Json.obj("message" -> s"Successful get products by userId $userId !!", "code" -> 200, "products" -> products))
    ).fallbackTo(errorInternal)
  }

  implicit val productStatusReads: Reads[(String, Int)] =
    (__ \ "productId").read[String] and
      (__ \ "status").read[Int] tupled

  def updateProductStatus() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, Int)] {
      case (productId, status) =>
        productService.updateProductStatus(productId, status).flatMap {
          case r if r.n > 0 => ok(Json.obj("message" -> "Successful update product !!", "code" -> 200))
          case r if r.n < 1 => ok(Json.obj("message" -> "Fail update product !!", "code" -> ERROR_BADREQUEST))
        }.fallbackTo(errorInternal)
    }
  }

  def getProductByProductId(productId: String) = SecuredApiAction { implicit request =>
    productService.getProductByProductId(productId).flatMap {
      case products if products.nonEmpty => ok(Json.obj("message" -> s"Successful get products by productId $productId !!", "code" -> 200, "products" -> products.head))
      case products if products.isEmpty => ok(Json.obj("message" -> s"Fail get products by productId $productId !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  implicit val upReads: Reads[(String, String, List[String], String, String, Product)] =
    (__ \ "userId").read[String] and
      (__ \ "productId").read[String] and
      (__ \ "phones").read[List[String]] and
      (__ \ "location").read[String] and
      (__ \ "address").read[String] and
      (__ \ "product").read[Product] tupled
  def updateProductById() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[(String, String, List[String], String, String, Product)] {
      case (userId, productId, phones, location, address, product) =>
        productService.updateProductInfo(
          Product(
            Some(BSONObjectID.parse(productId).get),
            product.productName,
            product.productDescription,
            Some(List()),
            product.price,
            product.discount,
            product.discountFromDate,
            product.discountEndDate,
            Some("normal"),
            Some(FormatDate.parseDate(FormatDate.printDate(new DateTime()))),
            Some(FormatDate.parseDate("1970-01-01")),
            product.status,
            Some(FormatDate.parseDate("1970-01-01"))
          )
        ).flatMap {
            case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail update product !!", "code" -> ERROR_BADREQUEST))
            case writeRes if writeRes.n > 0 =>
              userService.updateMemberProfile(userId, "", "", "", phones, location, address).flatMap {
                case r if r.n < 1 => ok(Json.obj("message" -> "Fail update member information !!", "code" -> ERROR_BADREQUEST))
                case r if r.n > 0 => ok(Json.obj("message" -> "Successful update member information !!", "code" -> 200))
              }.fallbackTo(errorInternal)
          }
    }
  }

  def updateImage(productId: String, productImage: String) = SecuredApiActionWithBodyFile { request =>
    request.body.file("file").map { file =>
      import java.io.File
      val ori_name: Array[String] = file.filename.split('.')
      val filename = ori_name(0) + (100000 + Random.nextInt(900000)).toString + "." + ori_name(1)
      //val contentType = file.contentType
      file.ref.moveTo(new File(ImageBase64.imagePath + filename))

      productService.getProductById(productId).flatMap {
        case None => ok(Json.obj("message" -> "Product not found !!", "code" -> ERROR_NOTFOUND))
        case Some(product) =>
          if (product.productImage.get.nonEmpty) {
            var images = product.productImage.get
            if (product.productImage.get.contains(productImage)) {
              images = product.productImage.get.updated(product.productImage.get.indexOf(productImage), filename)
            } else {
              images = product.productImage.get :+ filename
            }
            if (productImage != "example.png") {
              ImageBase64.removeFile(ImageBase64.imagePath + productImage)
            }
            productService.uploadProductImage(productId, images).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful update images !!", "code" -> 200, "image" -> filename))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail update images !!", "code" -> ERROR_BADREQUEST))
            }
          } else {
            productService.uploadProductImage(productId, List(filename)).flatMap {
              case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Successful update images !!", "code" -> 200, "image" -> filename))
              case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail update images !!", "code" -> ERROR_BADREQUEST))
            }
          }
      }
    }.getOrElse {
      ok(Json.obj("message" -> "Fail upload image !!", "code" -> ERROR_BADREQUEST))
    }
  }

  def removeImage(productId: String, productImage: String) = SecuredApiAction { implicit request =>
    productService.removeProductImage(productId, productImage).flatMap {
      case r if r.n > 0 =>
        ImageBase64.removeFile(ImageBase64.imagePath + productImage)
        ok(Json.obj("message" -> "Successful remove product image !!", "code" -> 200))
      case r if r.n < 1 =>
        ok(Json.obj("message" -> "Fail remove product image !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  def deleteProduct(productId: String) = SecuredApiAction { implicit request =>
    productService.getProductById(productId).flatMap {
      case None => ok(Json.obj("message" -> "Product not found !!", "code" -> ERROR_NOTFOUND))
      case Some(product) =>
        if (product.productImage.get.nonEmpty) {
          product.productImage.get.foreach(
            image =>
              ImageBase64.removeFile(ImageBase64.imagePath + image)
          )
        }
        productService.deleteProductById(productId).flatMap {
          case r if r.n > 0 =>
            storeService.removeProductId(productId).flatMap(
              result => ok(Json.obj("message" -> "Successful remove product !!", "code" -> 200))
            ).fallbackTo(errorInternal)
            categoryService.removeProductId(productId).flatMap(
              result => ok(Json.obj("message" -> "Successful remove product !!", "code" -> 200))
            ).fallbackTo(errorInternal)
            productViewService.deleteView(productId).flatMap(
              result => ok(Json.obj("message" -> "Successful remove product !!", "code" -> 200))
            ).fallbackTo(errorInternal)
          case r if r.n < 1 =>
            ok(Json.obj("message" -> "Fail remove product image !!", "code" -> ERROR_BADREQUEST))
        }.fallbackTo(errorInternal)
    }
  }

  def renewProduct(productId: String) = SecuredApiAction { implicit request =>
    productService.renewProductById(productId).flatMap {
      case writeRes if writeRes.n > 0 =>
        productViewService.clearView(productId).flatMap {
          case r if r.n > 0 => ok(Json.obj("message" -> "Successful renew product !", "code" -> 200))
          case r if r.n < 1 => ok(Json.obj("message" -> "Fail renew product !", "code" -> ERROR_BADREQUEST))
        }
      case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail renew product !", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  def countViewProduct(productId: String) = ApiAction { implicit request =>
    productViewService.getViewByProductId(productId, request.remoteAddress).flatMap {
      case views if views.nonEmpty =>
        val view = (views.head \ "viewer").as[Viewer]
        if ((view.viewDate.get plusHours 12).getMillis >= new DateTime().getMillis) {
          ok(Json.obj("message" -> s"Today you already viewed this product !!", "code" -> 200))
        } else {
          productViewService.increaseViews(productId, request.remoteAddress).flatMap {
            case r if r.n < 1 => ok(Json.obj("message" -> s"Fail increase product's view !!", "code" -> ERROR_BADREQUEST))
            case r if r.n > 0 => ok(Json.obj("message" -> s"Successful increase product's view !!", "code" -> 200))
          }
        }
      case views if views.isEmpty =>
        productViewService.updateViewByProductId(productId, Viewer(request.remoteAddress, Some(new DateTime()), 1)).flatMap {
          case r if r.n < 1 => ok(Json.obj("message" -> s"Fail update product's view !!", "code" -> ERROR_BADREQUEST))
          case r if r.n > 0 => ok(Json.obj("message" -> s"Successful update product's view !!", "code" -> 200))
        }
    }
  }

  /* sona */
  /* LIST ALL PRODUCTS BY USER ID  */
  def listPromotedProductByUserId(userId: String) = SecuredApiAction { implicit request =>
    productService.listPromoteProductsByUserId(userId).flatMap {
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

  /* GET PROMOTED PRODUCT BY PRODUCT ID  */
  def getPromotedProductById(id: String) = SecuredApiAction { implicit request =>
    productService.getPromotedProductByProductId(id).flatMap {
      case Some(product) =>
        ok(Json.obj(
          "message" -> "Product found",
          "code" -> 200,
          "product" -> product
        ))
      case None =>
        ok(Json.obj(
          "message" -> "Product not found",
          "code" -> 500
        ))
    }.fallbackTo(errorInternal)
  }

}

