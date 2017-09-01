package services.view

import com.google.inject.{ ImplementedBy, Inject }
import models.view.VisitorView
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import repositories.view.IVisitorViewRepo

import scala.concurrent.Future

@ImplementedBy(classOf[VisitorViewService])
trait IVisitorViewService {
  def insertVisitorView(view: VisitorView): Future[WriteResult]
  def getVisitorViews(ipAddress: String, fromDate: String, toDate: String): Future[List[VisitorView]]
  def getFilterVisitors(year: Int, month: Int, day: Int): Future[List[JsObject]]
}

class VisitorViewService @Inject() (val visitorViewRepo: IVisitorViewRepo) extends IVisitorViewService {

  override def insertVisitorView(view: VisitorView): Future[WriteResult] = visitorViewRepo.insertVisitorView(view)

  override def getVisitorViews(ipAddress: String, fromDate: String, toDate: String): Future[List[VisitorView]] = visitorViewRepo.getVisitorViews(ipAddress, fromDate, toDate)

  override def getFilterVisitors(year: Int, month: Int, day: Int): Future[List[JsObject]] = visitorViewRepo.getFilterVisitors(year, month, day)

}
