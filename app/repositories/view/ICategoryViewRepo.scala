package repositories.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.{ CategoryView, Viewer }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[CategoryViewRepo])
trait ICategoryViewRepo {
  def insertCategoryView(view: CategoryView): Future[WriteResult]
  def updateCategoryViewByCategoryId(categoryId: BSONObjectID, viewer: Viewer): Future[WriteResult]
  def increaseCategoryView(categoryId: BSONObjectID, ipAddress: String): Future[WriteResult]
  def clearCategoryView(categoryId: String): Future[WriteResult]
  def getCategoryViewByIdAndIp(categoryId: BSONObjectID, ipAddress: String): Future[List[JsObject]]
  def getCategoryViewById(categoryId: BSONObjectID): Future[Option[CategoryView]]
}

class CategoryViewRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends ICategoryViewRepo {
  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("category_view_tbl"))

  override def insertCategoryView(view: CategoryView): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(view))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new view")
    }
    writeRes
  }

  /**
   * Function updateCategoryViewByCategoryId use to push new viewer
   * @param categoryId
   * @param viewer
   * @return
   */
  override def updateCategoryViewByCategoryId(categoryId: BSONObjectID, viewer: Viewer): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("categoryId" -> categoryId),
      BSONDocument(
        "$push" -> BSONDocument(
          "viewer" -> BSONDocument(
            "ipAddress" -> viewer.ipAddress,
            "viewDate" -> new DateTime().getMillis,
            "views" -> viewer.views
          )
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful append category view with result $data")
    }
    writeRes
  }

  /**
   * Function increaseCategoryView use to increase number of category views
   * @param categoryId
   * @param ipAddress
   * @return
   */
  override def increaseCategoryView(categoryId: BSONObjectID, ipAddress: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("productId" -> categoryId, "viewer.ipAddress" -> ipAddress),
      BSONDocument(
        "$inc" -> BSONDocument(
          "viewer.$.views" -> 1
        ),
        "$set" -> BSONDocument(
          "viewer.$.viewDate" -> new DateTime().getMillis
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful increase category views with result $data")
    }
    writeRes
  }

  override def clearCategoryView(categoryId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("categoryId" -> BSONObjectID.parse(categoryId).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "viewer" -> List()
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful clear product view with result $data")
    }
    writeRes
  }

  override def getCategoryViewByIdAndIp(categoryId: BSONObjectID, ipAddress: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val views = collection.flatMap(_.aggregate(
      UnwindField("viewer"),
      List(
        Match(
          Json.obj(
            "categoryId" -> categoryId,
            "viewer.ipAddress" -> ipAddress
          )
        ),
        Sort(Descending("viewer.viewDate"))
      )
    ).map(_.head[JsObject]))
    views.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get category view with result $data")
    }
    views
  }

  override def getCategoryViewById(categoryId: BSONObjectID): Future[Option[CategoryView]] = {
    val view = collection.flatMap(_.find(Json.obj("categoryId" -> categoryId)).one[CategoryView])
    view.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful get category view by Id !!")
    }
    view
  }
}
