package services.rating

import com.google.inject.{ ImplementedBy, Inject }
import models.rating.{ Rater, StarRating }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.rating.IStarRatingRepo

import scala.concurrent.Future

@ImplementedBy(classOf[StarRatingService])
trait IStarRatingService {
  def insertRating(starRating: StarRating): Future[WriteResult]
  def increaseRater(productId: BSONObjectID, rater: Rater): Future[WriteResult]
  def updateStar(productId: BSONObjectID, rater: Rater): Future[WriteResult]
  def getStarRatingByProductId(productId: BSONObjectID): Future[Option[StarRating]]
  def getStarRatingByProductIdAndIp(productId: BSONObjectID, ipAddress: String): Future[List[JsObject]]
  def getTotalStarRatingByProductId(productId: BSONObjectID): Future[List[JsObject]]
}

class StarRatingService @Inject() (val starRatingRepo: IStarRatingRepo) extends IStarRatingService {

  override def insertRating(starRating: StarRating): Future[WriteResult] = starRatingRepo.insertRating(starRating)

  override def increaseRater(productId: BSONObjectID, rater: Rater): Future[WriteResult] = starRatingRepo.increaseRater(productId, rater)

  override def updateStar(productId: BSONObjectID, rater: Rater): Future[WriteResult] = starRatingRepo.updateStar(productId, rater)

  override def getStarRatingByProductId(productId: BSONObjectID): Future[Option[StarRating]] = starRatingRepo.getStarRatingByProductId(productId)

  override def getStarRatingByProductIdAndIp(productId: BSONObjectID, ipAddress: String): Future[List[JsObject]] = starRatingRepo.getStarRatingByProductIdAndIp(productId, ipAddress)

  override def getTotalStarRatingByProductId(productId: BSONObjectID): Future[List[JsObject]] = starRatingRepo.getTotalStarRatingByProductId(productId)
}
