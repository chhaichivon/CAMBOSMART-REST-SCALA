package controllers

import akka.actor.ActorSystem
import api.ApiError._
import com.google.inject.Inject
import models.rating.{ Rater, StarRating }
import play.api.i18n.MessagesApi
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import services.rating.IStarRatingService

import scala.concurrent.ExecutionContext.Implicits.global

class StarRatingController @Inject() (val starRatingService: IStarRatingService, val messagesApi: MessagesApi, system: ActorSystem) extends api.ApiController {

  def insertRating() = ApiActionWithBody { implicit request =>
    readFromRequest[StarRating] { starRating =>
      starRatingService.getStarRatingByProductId(starRating.productId).flatMap {
        case None =>
          val starRatingData: StarRating = new StarRating(
            starRating._id,
            starRating.productId,
            List(new Rater(
              request.remoteAddress,
              starRating.raters.head.star
            ))
          )
          starRatingService.insertRating(starRatingData).flatMap(
            result => ok(Json.obj(
              "message" -> "Successful insert new star rating !!",
              "code" -> 200
            ))
          ).fallbackTo(errorInternal)
        case Some(rater) =>
          starRatingService.getStarRatingByProductIdAndIp(rater.productId, request.remoteAddress).flatMap {
            case raters if raters.nonEmpty =>
              val raterData: Rater = new Rater(
                request.remoteAddress,
                starRating.raters.head.star
              )
              val r = (raters.head \ "raters").as[Rater]
              if (r.ip == request.remoteAddress) {
                starRatingService.updateStar(rater.productId, raterData).flatMap(
                  result => ok(Json.obj(
                    "message" -> "Successful update star in star rating !!",
                    "code" -> 200
                  ))
                ).fallbackTo(errorInternal)
              } else {
                starRatingService.increaseRater(starRating.productId, raterData).flatMap(
                  result => ok(Json.obj(
                    "message" -> "Successful increase rater !!",
                    "code" -> 200
                  ))
                ).fallbackTo(errorInternal)
              }
            case raters if raters.isEmpty =>
              val raterData: Rater = new Rater(
                request.remoteAddress,
                starRating.raters.head.star
              )
              starRatingService.increaseRater(starRating.productId, raterData).flatMap(
                result => ok(Json.obj(
                  "message" -> "Successful increase rater !!",
                  "code" -> 200
                ))
              ).fallbackTo(errorInternal)
          }
      }
    }
  }

  def getStarRatingByProductIdAndIp(productId: String) = ApiAction { implicit request =>
    val ipAddress: String = request.remoteAddress
    starRatingService.getStarRatingByProductIdAndIp(BSONObjectID.parse(productId).get, ipAddress).flatMap(
      result => ok(result)
    ).fallbackTo(errorInternal)
  }

  def getTotalStarRatingByProductId(productId: String) = ApiAction { implicit request =>
    starRatingService.getTotalStarRatingByProductId(BSONObjectID.parse(productId).get).flatMap(
      result => ok(result)
    ).fallbackTo(errorInternal)
  }
}
