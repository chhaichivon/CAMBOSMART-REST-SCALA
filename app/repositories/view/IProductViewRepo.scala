package repositories.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.{ ProductView, Viewer }
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.Json._
import play.api.libs.json._
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[ProductViewRepo])
trait IProductViewRepo {
  def insertView(view: ProductView): Future[WriteResult]
  def updateViewByProductId(productId: String, viewer: Viewer): Future[WriteResult]
  def increaseViews(productId: String, ipAddress: String): Future[WriteResult]
  def clearView(productId: String): Future[WriteResult]
  def deleteView(productId: String): Future[WriteResult]
  def getViewByProductId(productId: String, ipAddress: String): Future[List[JsObject]]
}

class ProductViewRepo @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends IProductViewRepo {

  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("product_view_tbl"))

  override def insertView(view: ProductView): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(view))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successful insert new view")
    }
    writeRes
  }

  override def updateViewByProductId(productId: String, viewer: Viewer): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("productId" -> BSONObjectID.parse(productId).get),
      BSONDocument(
        "$push" -> BSONDocument(
          "viewer" -> BSONDocument("ipAddress" -> viewer.ipAddress, "viewDate" -> new DateTime().getMillis, "views" -> viewer.views)
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful append product view with result $data")
    }
    writeRes
  }

  override def increaseViews(productId: String, ipAddress: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("productId" -> BSONObjectID.parse(productId).get, "viewer.ipAddress" -> ipAddress),
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
      case Success(data) => println(s"Successful increase product views with result $data")
    }
    writeRes
  }

  override def clearView(productId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("productId" -> BSONObjectID.parse(productId).get),
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

  override def deleteView(productId: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.remove(obj("productId" -> BSONObjectID.parse(productId).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("Successfully delete product view !!")
    }
    writeRes
  }

  override def getViewByProductId(productId: String, ipAddress: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val views = collection.flatMap(_.aggregate(
      UnwindField("viewer"),
      List(
        Match(
          Json.obj(
            "productId" -> BSONObjectID.parse(productId).get,
            "viewer.ipAddress" -> ipAddress
          )
        )
      )
    ).map(_.head[JsObject]))
    views.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get product view with result $data")
    }
    views
  }

}