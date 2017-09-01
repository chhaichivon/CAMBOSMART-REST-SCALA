package services.mistake

import com.google.inject.{ Inject, ImplementedBy }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import repositories.mistake.IMistakeRepo
import scala.concurrent.Future
import models.mistake.Mistake

@ImplementedBy(classOf[MistakeService])
trait IMistakeService {
  def insertMistake(mistake: Mistake): Future[WriteResult]
  def listMistakes(page: Int, limit: Int): Future[List[JsObject]]
  def updateMistakeById(id: String, mistake: Mistake): Future[WriteResult]
  def deleteMistake(id: String): Future[WriteResult]
}

class MistakeService @Inject() (val mistakeRepo: IMistakeRepo) extends IMistakeService {

  override def insertMistake(mistake: Mistake): Future[WriteResult] = mistakeRepo.insertMistake(mistake)

  override def listMistakes(page: Int, limit: Int): Future[List[JsObject]] = mistakeRepo.listMistakes(page, limit)

  override def updateMistakeById(id: String, mistake: Mistake): Future[WriteResult] = mistakeRepo.updateMistakeById(id, mistake)

  override def deleteMistake(id: String): Future[WriteResult] = mistakeRepo.deleteMistake(id)
}
