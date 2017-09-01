package controllers
import java.io.File

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.category.Category
import models.view.{ CategoryView, Viewer }
import org.joda.time.DateTime
import services.category.ICategoryService
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import services.view.ICategoryViewService

import scala.concurrent.ExecutionContext.Implicits.global

class ProductCategory @Inject() (categoryService: ICategoryService, categoryViewService: ICategoryViewService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  /**
   * Function saveCategory
   * @return
   */
  def insertCategory() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Category] { category =>
      categoryService.getCategoryByName(category.categoryName).flatMap {
        case None =>
          // check category icon exist
          if (category.categoryIcon.get != "") {
            categoryService.getCategoryByIcon(category.categoryIcon.get).flatMap {
              case None =>
                categoryService.insertCategory(
                  Category(
                    Some(BSONObjectID.generate()),
                    category.productId,
                    category.categoryName,
                    category.khName,
                    category.categoryDescription,
                    category.parentId,
                    category.ancestorId,
                    category.categoryIcon,
                    category.commonCategory,
                    Some(0)
                  )
                ).flatMap {
                    case writeResult if writeResult.n > 0 => ok(Json.obj("message" -> "Successful add new category !!", "code" -> 200))
                    case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new category !!", "code" -> ERROR_BADREQUEST))
                  }.fallbackTo(errorInternal)
              case Some(data) =>
                ok(Json.obj("message" -> "Category icon is already exist !!", "code" -> 208))
            }.fallbackTo(errorInternal)
          } else {
            categoryService.insertCategory(
              Category(
                Some(BSONObjectID.generate()),
                category.productId,
                category.categoryName,
                category.khName,
                category.categoryDescription,
                category.parentId,
                category.ancestorId,
                category.categoryIcon,
                category.commonCategory,
                Some(0)
              )
            ).flatMap {
                case writeResult if writeResult.n > 0 => ok(Json.obj("message" -> "Successful add new category !!", "code" -> 200))
                case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail add new category !!", "code" -> ERROR_BADREQUEST))
              }.fallbackTo(errorInternal)
          }
        case Some(cat) => ok(Json.obj("message" -> "Category is already exist.", "code" -> 209))
      }.fallbackTo(errorInternal)
    }
  }

  /**
   * Function updateCategoryById
   * @return
   */
  def updateCategory() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[Category] { category =>
      categoryService.updateCategory(category).flatMap {
        case writeResult if writeResult.n > 0 => ok(Json.obj("message" -> "Successful update category.", "code" -> 200))
        case writeResult if writeResult.n < 1 => ok(Json.obj("message" -> "Fail with update category.", "code" -> ERROR_BADREQUEST))
      }.fallbackTo(errorInternal)
    }
  }

  /**
   * Function deleteCategoryById
   * @param id
   * @return
   */
  def deletedCategory(id: String) = SecuredApiAction { implicit request =>
    categoryService.deleteCategory(id).flatMap {
      case writeRes if writeRes.n > 0 => ok(Json.obj("message" -> "Category has been deleted.", "code" -> 200))
      case writeRes if writeRes.n < 1 => ok(Json.obj("message" -> "Fail with delete category", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  def getCategory(id: String) = SecuredApiAction { implicit request =>
    categoryService.getCategoryById(id).flatMap {
      case Some(category) => ok(Json.obj("message" -> "Successful get category by id !!", "code" -> 200, "category" -> category))
      case None => ok(Json.obj("message" -> "Fail get category by id !!", "code" -> ERROR_BADREQUEST))
    }.fallbackTo(errorInternal)
  }

  /**
   * Function listParentCategories
   * @return
   */
  def listParentCategories(page: Int, limit: Int) = ApiAction { implicit request =>
    categoryService.listParentCategory(page, limit).flatMap { categories =>
      ok(Json.obj(
        "message" -> "Categories found !!",
        "code" -> 200,
        "categories" -> categories
      ))
    }.fallbackTo(errorInternal)
  }

  /**
   * Function listSubCategories
   * @param parentId
   * @param page
   * @param limit
   * @return
   */
  def listSubCategories(parentId: String, page: Int, limit: Int) = ApiAction { implicit request =>
    categoryService.listSubCategoryById(parentId, page, limit).flatMap { categories =>
      ok(Json.obj(
        "message" -> "Categories found !!",
        "code" -> 200,
        "categories" -> categories
      ))
    }.fallbackTo(errorInternal)
  }

  /* LIST CHILD CATEGORY BY PARENT ID */
  def listChildCategoriesByParentId(id: String, ancestor: Int, page: Int, limit: Int) = SecuredApiAction { implicit request =>
    categoryService.listChildCategoryByParentId(id, ancestor, page, limit).flatMap {
      case categories if categories.isEmpty => ok(Json.obj("message" -> "Categories is not found", "code" -> 204))
      case categories if categories.nonEmpty =>
        ok(Json.obj(
          "message" -> "Categories found",
          "code" -> 200,
          "categories" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  /* LIST ALL CATEGORIES */
  def listAllCategories = ApiAction { implicit request =>
    categoryService.listAllCategory().flatMap {
      case null => ok(Json.obj("message" -> "Categories not found", "code" -> 204))
      case categories =>
        ok(Json.obj(
          "message" -> "Categories found",
          "code" -> 200,
          "categories" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  def listParentAndChild = ApiAction { implicit request =>
    categoryService.listParentAndChild().flatMap {
      case null => ok(Json.obj("message" -> "Categories not found", "code" -> ERROR_USER_NOTFOUND))
      case categories =>
        ok(Json.obj(
          "message" -> "Parent And Child Categories found",
          "code" -> 200,
          "categories" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  /* LIST PARENT CATEGORIES WITH PAGINATION */
  def listParentCategoriesWithPagination(page: Int, limit: Int) = SecuredApiAction { implicit request =>
    categoryService.listParentCategoryWithPagination(page, limit).flatMap {
      case categories if categories isEmpty =>
        ok(Json.obj("message" -> "Categories is not found", "code" -> 204))
      case categories if categories nonEmpty =>
        ok(Json.obj(
          "message" -> "Categories found",
          "code" -> 200,
          "categories" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  /* GET CATEGORY BY NAME */
  def getCategoryByName(name: String) = SecuredApiAction { implicit request =>
    categoryService.getCategoryByName(name).flatMap {
      case None => ok(Json.obj("message" -> "Category not found", "code" -> 204))
      case Some(category) =>
        ok(Json.obj(
          "message" -> "Category found",
          "code" -> 200,
          "category" -> Json.obj(
            "_id" -> category._id.get.stringify,
            "categoryName" -> category.categoryName,
            "categoryDescription" -> category.categoryDescription,
            "categoryIcon" -> category.categoryIcon
          )
        ))
    }.fallbackTo(errorInternal)
  }

  /* GET CATEGORY BY ID */
  def getCategoryById(id: String) = SecuredApiAction { implicit request =>
    categoryService.getCategoryById(id).flatMap {
      case None => ok(Json.obj("message" -> "Category not found", "code" -> 204))
      case Some(category) =>
        ok(Json.obj(
          "message" -> "Category found",
          "code" -> 200,
          "category" -> Json.obj(
            "_id" -> category._id.get.stringify,
            "categoryName" -> category.categoryName,
            "categoryDescription" -> category.categoryDescription,
            "categoryIcon" -> category.categoryIcon,
            "categoryCommon" -> category.commonCategory
          )
        ))
    }.fallbackTo(errorInternal)
  }

  /* FILTER CATEGORIES BY NAME */
  def filterCategoriesByName(name: String) = SecuredApiAction { implicit request =>
    categoryService.filterCategoryByName(name).flatMap {
      case null => ok(Json.obj("message" -> "Category is not found", "code" -> 204))
      case categories =>
        ok(Json.obj(
          "message" -> "Category found",
          "code" -> 200,
          "category" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  def getThirdCategories(name: String) = ApiAction { implicit request =>
    categoryService.getThirdCategoryByName(name).flatMap(
      categories => ok(
        Json.obj(
          "message" -> "Successful get third category !!",
          "code" -> 200,
          "categories" -> categories
        )
      )
    ).fallbackTo(errorInternal)
  }

  def checkCategory(name: String) = ApiAction { implicit request =>
    categoryService.checkSubCategory(name).flatMap {
      case null => ok(Json.obj("message" -> "check category", "code" -> 204))
      case categories =>
        ok(Json.obj(
          "message" -> "Check Category Found",
          "code" -> 200,
          "category" -> categories
        ))
    }.fallbackTo(errorInternal)
  }

  def countViewCategory(categoryId: String) = ApiAction { implicit request =>
    categoryViewService.getCategoryViewById(BSONObjectID.parse(categoryId).get).flatMap {
      case None =>
        categoryViewService.insertCategoryView(
          CategoryView(
            Some(BSONObjectID.generate()),
            Some(BSONObjectID.parse(categoryId).get),
            List(Viewer(request.remoteAddress, Some(new DateTime()), 1)),
            Some(0)
          )
        ).flatMap {
            case r if r.n < 1 => ok(Json.obj("message" -> s"Fail insert category's view !!", "code" -> ERROR_BADREQUEST))
            case r if r.n > 0 => ok(Json.obj("message" -> s"Successful insert category's view !!", "code" -> 200))
          }
      case Some(data) =>
        categoryViewService.getCategoryViewByIdAndIp(BSONObjectID.parse(categoryId).get, request.remoteAddress).flatMap {
          case views if views.nonEmpty =>
            val view = (views.head \ "viewer").as[Viewer]
            if ((view.viewDate.get plusHours 12).getMillis >= new DateTime().getMillis) {
              ok(Json.obj("message" -> s"Today you already viewed this category !!", "code" -> 200))
            } else {
              categoryViewService.increaseCategoryView(BSONObjectID.parse(categoryId).get, request.remoteAddress).flatMap {
                case r if r.n < 1 => ok(Json.obj("message" -> s"Fail increase category's view !!", "code" -> ERROR_BADREQUEST))
                case r if r.n > 0 => ok(Json.obj("message" -> s"Successful increase category's view !!", "code" -> 200))
              }
            }
          case views if views.isEmpty =>
            categoryViewService.updateCategoryViewByCategoryId(BSONObjectID.parse(categoryId).get, Viewer(request.remoteAddress, Some(new DateTime()), 1)).flatMap {
              case r if r.n < 1 => ok(Json.obj("message" -> s"Fail update category's view !!", "code" -> ERROR_BADREQUEST))
              case r if r.n > 0 => ok(Json.obj("message" -> s"Successful update category's view !!", "code" -> 200))
            }
        }
    }
  }

  def listPopularCategory(limit: Int) = ApiAction { implicit request =>
    categoryService.listPopularCategory(limit).flatMap { categories =>
      ok(Json.obj(
        "message" -> "Category found !!",
        "code" -> 200,
        "categories" -> categories
      ))
    }.fallbackTo(errorInternal)
  }

}
