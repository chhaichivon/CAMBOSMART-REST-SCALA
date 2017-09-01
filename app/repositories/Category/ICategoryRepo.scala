package repositories.category
import javax.inject.Inject
import com.google.inject.ImplementedBy
import models.category.Category
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.{ QueryOpts, Cursor, ReadPreference }
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[CategoryRepo])
trait ICategoryRepo {

  def insertCategory(category: Category): Future[WriteResult]

  def updateCategory(category: Category): Future[WriteResult]

  def deleteCategory(id: String): Future[WriteResult]

  def getCategoryById(id: String): Future[Option[Category]]

  def getCategoryByName(name: String): Future[Option[Category]]

  def getCategoryByIcon(icon: String): Future[Option[Category]]

  def listParentCategory(page: Int, limit: Int): Future[List[Category]]

  def listSubCategoryById(parentId: String, page: Int, limit: Int): Future[List[Category]]

  def listPopularCategory(limit: Int): Future[List[JsObject]]

  def removeProductId(productId: String): Future[WriteResult]

  /**----------------------------------END---------------------------------------------*/

  def listChildCategoryByParentId(parentId: String, ancestor: Int, page: Int, limit: Int): Future[List[JsObject]]
  def listAllCategory(): Future[List[JsObject]]
  def listParentAndChild(): Future[List[JsObject]]
  def listParentCategoryWithPagination(page: Int, limit: Int): Future[List[JsObject]]
  def filterCategoryByName(name: String): Future[List[JsObject]]
  /*oudam*/
  def updateProductIdInCategory(id: String, productId: List[BSONObjectID]): Future[WriteResult]
  def getThirdCategoryByName(name: String): Future[List[JsObject]]
  /* naseat */
  def checkSubCategory(name: String): Future[List[JsObject]]
}

class CategoryRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends ICategoryRepo {

  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("category_tbl"))

  /**
   * Function insertCategory
   * @param category
   * @return
   */
  override def insertCategory(category: Category): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(category))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new category")
    }
    writeRes
  }

  /**
   * Function updateCategory
   * @param category
   * @return
   */
  override def updateCategory(category: Category): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> category._id),
      BSONDocument(
        "$set" -> BSONDocument(
          "categoryName" -> category.categoryName,
          "khName" -> category.khName,
          "categoryDescription" -> category.categoryDescription,
          "categoryIcon" -> category.categoryIcon,
          "commonCategory" -> category.commonCategory
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully update category with result $data")
    }
    writeRes
  }

  /**
   * Function deleteCategory
   * @param id
   * @return
   */
  override def deleteCategory(id: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj(
        "$or" -> List(
          Json.obj("_id" -> BSONObjectID.parse(id).get),
          Json.obj("parentId" -> BSONObjectID.parse(id).get),
          Json.obj("ancestorId" -> BSONObjectID.parse(id).get)
        )
      ),
      BSONDocument(
        "$set" -> BSONDocument(
          "deleted" -> 1
        )
      ),
      multi = true
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully deleted category with result $data")
    }
    writeRes
  }

  /**
   * Function getCategoryById
   * @param id
   * @return
   */
  override def getCategoryById(id: String): Future[Option[Category]] = {
    val category = collection.flatMap(_.find(Json.obj("_id" -> BSONObjectID.parse(id).get, "deleted" -> 0)).one[Category])
    category.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully get category by Id")
    }
    category
  }

  /**
   * Function listParentCategory
   * @param page
   * @param limit
   * @return
   */
  override def listParentCategory(page: Int, limit: Int): Future[List[Category]] = {
    def getLimit: Int = {
      if (limit > 0) {
        limit
      } else {
        Int.MaxValue
      }
    }
    val categories = collection.flatMap(_.find(
      Json.obj(
        "parentId" -> BSONObjectID.parse("000000000000000000000000").get,
        "deleted" -> 0
      )
    ) //Json.obj("$exists" -> false)
      .sort(Json.obj("categoryName" -> 1))
      .options(QueryOpts(skipN = (page - 1) * limit))
      .cursor[Category](ReadPreference.primary)
      .collect[List](getLimit, Cursor.FailOnError[List[Category]]()))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all parent categories !!")
    }
    categories
  }

  /**
   * Function listSubCategoryById
   * @param parentId
   * @param page
   * @param limit
   * @return
   */
  override def listSubCategoryById(parentId: String, page: Int, limit: Int): Future[List[Category]] = {
    val categories = collection.flatMap(_.find(
      Json.obj(
        "parentId" -> BSONObjectID.parse(parentId).get,
        "deleted" -> 0
      )
    )
      .options(QueryOpts(skipN = (page - 1) * limit))
      .cursor[Category](ReadPreference.primary)
      .collect[List](limit, Cursor.FailOnError[List[Category]]()))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all sub categories !!")
    }
    categories
  }

  /**
   * Function listPopularCategory
   * @param limit
   * @return
   */
  override def listPopularCategory(limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val categories = collection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "sub"
      ),
      List(
        UnwindField("sub"),
        Lookup(
          "category_view_tbl",
          "sub._id",
          "categoryId",
          "categoryView"
        ),
        UnwindField("categoryView"),
        Project(
          Json.obj(
            "categoryName" -> 1,
            "khName" -> 1,
            "sub" -> Json.obj("categoryName" -> "$sub.categoryName", "khName" -> "$sub.khName"),
            "views" -> Json.obj("$sum" -> "$categoryView.viewer.views")
          )
        ),
        Limit(limit),
        Sort(Descending("views"))
      )
    ).map(_.head[JsObject]))

    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get popular categories !!")
    }
    categories
  }

  override def removeProductId(productId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("productId" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$pull" -> BSONDocument(
          "productId" -> BSONObjectID.parse(productId).get
        )
      ),
      multi = true
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully remove productId from category !!")
    }
    writeRes
  }
  /**--------------------------------------------END-----------------------------------------*/

  /* LIST CHILD CATEGORY BY PARENT */
  override def listChildCategoryByParentId(parentId: String, ancestor: Int, page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val skip = (page * limit) - limit
    val categories = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "categoryName" -> 1,
          "categoryDescription" -> 1,
          "parentId" -> 1,
          "ancestorId" -> 1,
          "categoryIcon" -> 1,
          "commonCategory" -> 1,
          "deleted" -> 1,
          "numberOfAncestors" -> Json.obj("$size" -> "$ancestorId")
        )
      ),
      List(
        Match(Json.obj("parentId" -> BSONObjectID.parse(parentId).get, "deleted" -> 0, "numberOfAncestors" -> ancestor)),
        Skip(skip),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get categories by parentId")
    }
    categories
  }

  /* LIST ALL CATEGORIES */
  override def listAllCategory(): Future[List[JsObject]] = {
    val categories = collection.flatMap(_.find(Json.obj("deleted" -> 0))
      .projection(
        Json.obj(
          "categoryName" -> 1,
          "categoryDescription" -> 1,
          "categoryIcon" -> 1,
          "parentId" -> 1,
          "ancestorId" -> 1,
          "commonCategory" -> 1,
          "hit" -> 1
        )
      )
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful all categories")
    }
    categories
  }

  override def listParentAndChild(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val categories = collection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "children"
      ),
      List(
        UnwindField("children"),
        Match(Json.obj("parentId" -> BSONObjectID.parse("000000000000000000000000").get, "deleted" -> 0)),
        Project(
          Json.obj(
            "categoryName" -> 1,
            "khName" -> 1,
            "categoryIcon" -> 1,
            "commonCategory" -> 1,
            "children" -> Json.obj(
              "id" -> "$children._id",
              "categoryName" -> "$children.categoryName",
              "khName" -> "$children.khName"
            )
          )
        ),
        Group(
          Json.obj(
            "_id" -> "$_id",
            "categoryName" -> "$categoryName",
            "khName" -> "$khName",
            "categoryIcon" -> "$categoryIcon",
            "commonCategory" -> "$commonCategory"
          )
        )(
            "children" -> PushField("children")
          ),
        Project(
          Json.obj(
            "_id" -> 0,
            "id" -> "$_id._id",
            "categoryName" -> "$_id.categoryName",
            "khName" -> "$_id.khName",
            "categoryIcon" -> "$_id.categoryIcon",
            "commonCategory" -> "$_id.commonCategory",
            "childrenEn" -> "$children.categoryName",
            "childrenKh" -> "$children.khName",
            "sub" -> "$children"
          )
        ),
        Sort(Ascending("categoryName"))
      )
    ).map(_.head[JsObject]))

    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all categories with result")
    }
    categories
  }

  override def listParentCategoryWithPagination(page: Int, limit: Int): Future[List[JsObject]] = {
    val skip = (page * limit) - limit
    val categories = collection.flatMap(_.find(Json.obj("parentId" -> BSONObjectID.parse("000000000000000000000000").get, "deleted" -> 0)) //Json.obj("$exists" -> false)
      .projection(Json.obj(
        "categoryName" -> 1,
        "categoryDescription" -> 1,
        "categoryIcon" -> 1,
        "commonCategory" -> 1
      ))
      .options(QueryOpts(skipN = skip))
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](limit, Cursor.FailOnError[List[JsObject]]()))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get all parent categories")
    }
    categories
  }

  /* SEARCH CATEGORY BY NAME */
  override def getCategoryByName(name: String): Future[Option[Category]] = {
    val category = collection.flatMap(_.find(Json.obj(
      "categoryName" -> name,
      "deleted" -> 0
    )).one[Category])
    category.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully found category by name")
    }
    category
  }

  /* GET CATEGORY BY ICON */
  override def getCategoryByIcon(icon: String): Future[Option[Category]] = {
    val category = collection.flatMap(_.find(Json.obj(
      "categoryIcon" -> icon,
      "deleted" -> 0
    )).one[Category])
    category.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successfully found category by icon")
    }
    category
  }

  /* FILTER CATEGORIES BY NAME */
  override def filterCategoryByName(name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val categories = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "categoryName" -> Json.obj("$toLower" -> "$categoryName"),
          "categoryDescription" -> 1,
          "parentId" -> 1,
          "ancestorId" -> 1,
          "categoryIcon" -> 1,
          "commonCategory" -> 1,
          "deleted" -> 1
        )
      ),
      List(
        Match(Json.obj(
          "categoryName" -> Json.obj("$regex" -> ("^" + name.toLowerCase + ".*")),
          "deleted" -> 0
        ))
      )
    ).map(_.head[JsObject]))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful filter category by name" + data)
    }
    categories
  }

  /*oudam*/
  override def updateProductIdInCategory(id: String, productId: List[BSONObjectID]): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "productId" -> productId
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful update productId in category with result $data !!")
    }
    writeRes
  }

  override def getThirdCategoryByName(name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val categories = collection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "children"
      ),
      List(
        UnwindField("children"),
        Match(Json.obj("categoryName" -> name, "deleted" -> 0)),
        Project(
          Json.obj(
            "_id" -> 0,
            "categoryId" -> "$children._id",
            "categoryName" -> "$children.categoryName",
            "khName" -> "$children.khName"
          )
        )
      )
    ).map(_.head[JsObject]))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get categories by name $data")
    }
    categories
  }

  override def checkSubCategory(name: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val categories = collection.flatMap(_.aggregate(
      Lookup(
        "category_tbl",
        "_id",
        "parentId",
        "subCategory"
      ),
      List(
        UnwindField("subCategory"),
        Match(Json.obj("categoryName" -> name)),
        Project(
          Json.obj(
            "_id" -> 0,
            "sub" -> "$subCategory.categoryName"
          )
        )
      )
    ).map(_.head[JsObject]))
    categories.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful Find Sub category" + data)
    }
    categories
  }
}