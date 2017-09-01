package services.promoted

import com.google.inject.{ Inject, ImplementedBy }
import models.promoted.PromoteUser
import play.api.libs.json.JsObject
import models.store.Store
import models.product.Product
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.promoted.{ IPromoteMemberRepo }
import scala.concurrent.Future

/**
 * Created by Ky Sona on 5/3/2017.
 */
@ImplementedBy(classOf[PromoteUserService])
trait IPromoteUserService {
  def checkMemberRequest(id: BSONObjectID): Future[Option[PromoteUser]]
  def insertMemberRequest(promote: PromoteUser): Future[WriteResult]
  def listMemberRequests(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]]
  def promoteMemberById(id: String, userId: String, userType: String, startDate: String, endDate: String): Future[WriteResult]
  def deleteMemberRequest(id: String): Future[WriteResult]
  def listMerchantExpired(page: Int, limit: Int): Future[List[JsObject]]
  def updateExpiredMerchants(): Future[List[JsObject]]
  def listMemberRequestsExpired(page: Int, limit: Int): Future[List[JsObject]]
  def deleteMemberRequestExpired(): Future[List[JsObject]]
  def checkMerchantExpired(userId: String): Future[Option[PromoteUser]]
}

class PromoteUserService @Inject() (val promoteUserRepo: IPromoteMemberRepo) extends IPromoteUserService {
  override def checkMemberRequest(id: BSONObjectID): Future[Option[PromoteUser]] = promoteUserRepo.checkMemberRequest(id)

  override def insertMemberRequest(promote: PromoteUser): Future[WriteResult] = promoteUserRepo.insertMemberRequest(promote)

  override def listMemberRequests(name: String, location: String, startDate: String, endDate: String, page: Int, limit: Int): Future[List[JsObject]] = promoteUserRepo.listMemberRequests(name, location, startDate, endDate, page, limit)

  override def promoteMemberById(id: String, userId: String, userType: String, startDate: String, endDate: String): Future[WriteResult] = promoteUserRepo.promoteMemberById(id, userId, userType, startDate, endDate)

  override def deleteMemberRequest(id: String): Future[WriteResult] = promoteUserRepo.deleteMemberRequest(id)

  override def listMerchantExpired(page: Int, limit: Int): Future[List[JsObject]] = promoteUserRepo.listMerchantExpired(page, limit)

  override def updateExpiredMerchants(): Future[List[JsObject]] = promoteUserRepo.updateExpiredMerchants()

  override def listMemberRequestsExpired(page: Int, limit: Int): Future[List[JsObject]] = promoteUserRepo.listMemberRequestsExpired(page, limit)

  override def deleteMemberRequestExpired(): Future[List[JsObject]] = promoteUserRepo.deleteMemberRequestExpired()

  override def checkMerchantExpired(userId: String): Future[Option[PromoteUser]] = promoteUserRepo.checkMerchantExpired(userId)
}
