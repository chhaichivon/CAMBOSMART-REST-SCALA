package services.reports

import com.google.inject.{ Inject, ImplementedBy }
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.reports.{ IPromoteMemberReportRepo }
import scala.concurrent.Future
/**
 * Created by Ky Sona on 4/18/2017.
 */
@ImplementedBy(classOf[PromoteMemberReportService])
trait IPromoteMemberReportService {
  def listGrandTotalPromoteMemberIncome(startDate: String, endDate: String): Future[List[JsObject]]
  def listDetailPromoteMemberIncome(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]
}

class PromoteMemberReportService @Inject() (val promoteReport: IPromoteMemberReportRepo) extends IPromoteMemberReportService {
  override def listGrandTotalPromoteMemberIncome(startDate: String, endDate: String): Future[List[JsObject]] =
    promoteReport.listGrandTotalPromoteMemberIncome(startDate, endDate)

  override def listDetailPromoteMemberIncome(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] =
    promoteReport.listDetailPromoteMemberIncome(name, location, startDate, endDate, page, limit)

}
