package repositories.mistake

/**
 * Created by Naseat on 4/11/2017.
 */

import com.google.inject.{ ImplementedBy, Inject }
import models.mistake.Mistake
import play.api.libs.json.{ JsString, Json, JsObject }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{ AggregationFramework, WriteResult }
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

@ImplementedBy(classOf[MistakeRepo])
trait IMistakeRepo {
  def insertMistake(mistake: Mistake): Future[WriteResult]
  def listMistakes(page: Int, limit: Int): Future[List[JsObject]]
  def updateMistakeById(id: String, mistake: Mistake): Future[WriteResult]
  def deleteMistake(id: String): Future[WriteResult]
}

class MistakeRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IMistakeRepo {

  def mistakeCollection = reactiveMongoApi.database.map(_.collection[JSONCollection]("mistake_tbl"))

  override def insertMistake(mistake: Mistake): Future[WriteResult] = {
    val writeRes = mistakeCollection.flatMap(_.insert(mistake))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println("successful insert new mistake")
    }
    writeRes
  }

  override def updateMistakeById(id: String, mistake: Mistake): Future[WriteResult] = {
    val writeRes = mistakeCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "description" -> mistake.description,
          "mistakeDate" -> mistake.mistakeDate.get.getMillis
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful updated mistake")
    }
    writeRes
  }

  override def deleteMistake(id: String): Future[WriteResult] = {
    val writeRes = mistakeCollection.flatMap(_.update(
      Json.obj("_id" -> BSONObjectID.parse(id).get),
      BSONDocument(
        "$set" -> BSONDocument(
          "deleted" -> 1
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("Successful deleted  mistake")
    }
    writeRes
  }

  /* helper function */
  def getCountMistake(): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val total = mistakeCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "deleted" -> 1
        )
      ),
      List(
        Match(Json.obj("deleted" -> 1)),
        Group(JsString("$state"))("total" -> SumAll)
      )
    ).map(_.head[JsObject]))
    total.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("success to get count mistake")
    }
    total
  }

  override def listMistakes(page: Int, limit: Int): Future[List[JsObject]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val JsObj: List[JsObject] = Await.result(getCountMistake(), 10.seconds)
    var total: Int = 0
    if (JsObj.nonEmpty) {
      total = (JsObj.head \ "total").as[Int]
    } else {
      total = 0
    }
    println("total mistake count" + total)
    val skip = (page * limit) - limit
    val mistakes = mistakeCollection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "_id" -> 1,
          "mistakeType" -> 1,
          "mistakeDescription" -> 1,
          "mistakeDate" -> 1,
          "deleted" -> 1,
          "total" -> s"$total"
        )
      ),
      List(
        Match(Json.obj("deleted" -> 0)),
        Skip(skip),
        Limit(limit)
      )
    ).map(_.head[JsObject]))
    mistakes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println("successfully get mistake list")
    }
    mistakes
  }

}
