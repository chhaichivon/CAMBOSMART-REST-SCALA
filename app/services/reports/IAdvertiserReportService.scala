package services.reports

import com.google.inject.{ Inject, ImplementedBy }
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.reports.{ IAdvertiserReportRepo }
import scala.concurrent.Future
/**
 * Created by Ky Sona on 4/20/2017.
 */
@ImplementedBy(classOf[AdvertiserReportService])
trait IAdvertiserReportService {
  def listGrandTotalAdvertiserIncome(page: String, fromDate: String, toDate: String): Future[List[JsObject]]
  def listDetailAdvertiserIncome(page: String, city: String, fromDate: String, toDate: String, name: String, start: Int, limit: Int): Future[List[JsObject]]
  def listCategoryIncome(startDate: String, endDate: String, name: String): Future[List[JsObject]]
}

class AdvertiserReportService @Inject() (val advertiserReport: IAdvertiserReportRepo) extends IAdvertiserReportService {
  override def listGrandTotalAdvertiserIncome(page: String, fromDate: String, toDate: String): Future[List[JsObject]] = advertiserReport.listGrandTotalAdvertiserIncome(page, fromDate, toDate)

  override def listDetailAdvertiserIncome(page: String, city: String, fromDate: String, toDate: String, name: String, start: Int, limit: Int): Future[List[JsObject]] = advertiserReport.listDetailAdvertiserIncome(page, city, fromDate, toDate, name, start, limit)

  override def listCategoryIncome(startDate: String, endDate: String, name: String): Future[List[JsObject]] = advertiserReport.listCategoryIncome(startDate, endDate, name)
}
