package repositories.rating

import com.google.inject.{ ImplementedBy, Inject }
import models.rating.{ Rater, StarRating }
import play.api.libs.json.{ JsObject, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID, BSONString }
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

@ImplementedBy(classOf[StarRatingRepo])
trait IStarRatingRepo {
  def insertRating(starRating: StarRating): Future[WriteResult]
  def increaseRater(productId: BSONObjectID, rater: Rater): Future[WriteResult]
  def updateStar(productId: BSONObjectID, rater: Rater): Future[WriteResult]
  def getStarRatingByProductId(productId: BSONObjectID): Future[Option[StarRating]]
  def getStarRatingByProductIdAndIp(productId: BSONObjectID, ipAddress: String): Future[List[JsObject]]
  def getTotalStarRatingByProductId(productId: BSONObjectID): Future[List[JsObject]]
}

class StarRatingRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IStarRatingRepo {
  def ratingCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("rating_tbl"))

  override def insertRating(starRating: StarRating): Future[WriteResult] = {
    val writeResult = ratingCollection.flatMap(_.insert(starRating))
    writeResult.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful insert new star rating with result $data")
    }
    writeResult
  }
  override def increaseRater(productId: BSONObjectID, rater: Rater): Future[WriteResult] = {
    ratingCollection.flatMap(_.update(
      Json.obj("productId" -> productId),
      BSONDocument(
        "$push" -> BSONDocument(
          "raters" -> BSONDocument(
            "ip" -> rater.ip,
            "star" -> rater.star
          )
        )
      )
    ))
  }

  override def updateStar(productId: BSONObjectID, rater: Rater): Future[WriteResult] = {
    ratingCollection.flatMap(_.update(
      Json.obj("productId" -> productId, "raters.ip" -> rater.ip),
      BSONDocument(
        "$set" -> BSONDocument(
          "raters.$.star" -> rater.star
        )
      )
    ))
  }

  override def getStarRatingByProductId(productId: BSONObjectID): Future[Option[StarRating]] = {
    val starRating = ratingCollection.flatMap(_.find(Json.obj("productId" -> productId)).one[StarRating])
    starRating.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get star rating by productId with result $data")
    }
    starRating
  }

  override def getStarRatingByProductIdAndIp(productId: BSONObjectID, ipAddress: String): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val starRating = ratingCollection.flatMap(_.aggregate(
      UnwindField("raters"),
      List(
        Project(Json.obj(
          "productId" -> 1,
          "raters" -> 1
        )),
        Match(
          Json.obj(
            "productId" -> productId,
            "raters.ip" -> ipAddress
          )
        )
      )
    ).map(_.head[JsObject]))
    starRating.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get star rating with result $data")
    }
    starRating
  }

  override def getTotalStarRatingByProductId(productId: BSONObjectID): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val starRating = ratingCollection.flatMap(_.aggregate(
      UnwindField("raters"),
      List(
        Match(Json.obj(
          "productId" -> productId
        )),
        Group(Json.obj(
          "productId" -> "$productId",
          "star" -> "$raters.star"
        ))("totalCount" -> SumAll),
        Project(Json.obj(
          "_id" -> 0,
          "productId" -> "$_id.productId",
          "star" -> "$_id.star",
          "amount" -> "$totalCount",
          "total" -> Json.obj("$multiply" -> List("$_id.star", "$totalCount"))
        )),
        Group(Json.obj(
          "productId" -> "$productId"
        ))("detail" -> Push(Json.obj("star" -> "$star", "amount" -> "$amount", "total" -> "$total"))),
        Project(Json.obj(
          "_id" -> 0,
          "productId" -> "$_id.productId",
          "detail" -> "$detail"
        ))
      )
    ).map(_.head[JsObject]))
    starRating.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get total star rating with result $data")
    }
    starRating
  }
}
