package services.reports

import com.google.inject.{ Inject, ImplementedBy }
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.reports.IBootProductReportRepo
import scala.concurrent.Future

/**
 * Created by Ky Sona on 4/5/2017.
 */
@ImplementedBy(classOf[BootProductReportService])
trait IBootProductReportService {
  def listBootProductIncomeGrand(startDate: String, endDate: String): Future[List[JsObject]]
  def listBootProductIncomeDetail(startDate: String, endDate: String, userType: String, page: Int, limit: Int): Future[List[JsObject]]
}

class BootProductReportService @Inject() (val promoteReport: IBootProductReportRepo) extends IBootProductReportService {
  override def listBootProductIncomeGrand(startDate: String, endDate: String): Future[List[JsObject]] =
    promoteReport.listBootProductIncomeGrand(startDate, endDate)

  override def listBootProductIncomeDetail(startDate: String, endDate: String, userType: String, page: Int, limit: Int): Future[List[JsObject]] =
    promoteReport.listBootProductIncomeDetail(startDate, endDate, userType, page, limit);
}
