package services.promoted

import com.google.inject.{ Inject, ImplementedBy }
import models.promoted.PromoteProductPackage
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import repositories.promoted.{ IPromoteProductPackageRepo }
import scala.concurrent.Future
/**
 * Created by Ky Sona on 3/15/2017.
 */
@ImplementedBy(classOf[PromoteProductPackageService])
trait IPromoteProductPackageService {
  def listPromotePackage(typePromote: String, page: Int, limit: Int): Future[List[JsObject]]
  def insertPromotePackage(promotePackage: PromoteProductPackage): Future[WriteResult]
  def getPromotePackageById(id: String): Future[Option[PromoteProductPackage]]
  def updatePromotePackageById(promotePackage: PromoteProductPackage): Future[WriteResult]
  def deletePromotePackageById(id: String): Future[WriteResult]
  def listAllPackages(): Future[List[JsObject]]
}

class PromoteProductPackageService @Inject() (val promotePackageRepo: IPromoteProductPackageRepo) extends IPromoteProductPackageService {
  override def listPromotePackage(typePromote: String, page: Int, limit: Int): Future[List[JsObject]] = promotePackageRepo.listPromotePackage(typePromote, page, limit)

  override def insertPromotePackage(promotePackage: PromoteProductPackage): Future[WriteResult] = promotePackageRepo.insertPromotePackage(promotePackage)

  override def deletePromotePackageById(id: String): Future[WriteResult] = promotePackageRepo.deletePromotePackageById(id)

  override def updatePromotePackageById(promotePackage: PromoteProductPackage): Future[WriteResult] = promotePackageRepo.updatePromotePackageById(promotePackage)

  override def getPromotePackageById(id: String): Future[Option[PromoteProductPackage]] = promotePackageRepo.getPromotePackageById(id)

  override def listAllPackages(): Future[List[JsObject]] = promotePackageRepo.listAllPackages()
}
