package services.promoted

import com.google.inject.{ Inject, ImplementedBy }
import models.promoted.PromoteUserPackage
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import repositories.promoted.{ IPromoteUserPackageRepo }

import scala.concurrent.Future

/**
 * Created by Ky Sona on 4/7/2017.
 */
@ImplementedBy(classOf[PromoteUserPackageService])
trait IPromoteUserPackageService {
  def insertUserPromotePackage(promoteUserPackage: PromoteUserPackage): Future[WriteResult]
  def listUserPromotePackage(page: Int, limit: Int): Future[List[JsObject]]
  def getUserPromotePackageById(id: String): Future[Option[PromoteUserPackage]]
  def updateUserPromotePackageById(promoteUserPackage: PromoteUserPackage): Future[WriteResult]
  def deleteUserPromotePackageById(id: String): Future[WriteResult]
  def listAllUserPromotePackages(): Future[List[JsObject]]
}

class PromoteUserPackageService @Inject() (val promoteUserPackageRepo: IPromoteUserPackageRepo) extends IPromoteUserPackageService {
  override def insertUserPromotePackage(promoteUserPackage: PromoteUserPackage): Future[WriteResult] = promoteUserPackageRepo.insertUserPromotePackage(promoteUserPackage)

  override def listAllUserPromotePackages(): Future[List[JsObject]] = promoteUserPackageRepo.listAllUserPromotePackages();

  override def updateUserPromotePackageById(promoteUserPackage: PromoteUserPackage): Future[WriteResult] = promoteUserPackageRepo.updateUserPromotePackageById(promoteUserPackage)

  override def getUserPromotePackageById(id: String): Future[Option[PromoteUserPackage]] = promoteUserPackageRepo.getUserPromotePackageById(id)

  override def listUserPromotePackage(page: Int, limit: Int): Future[List[JsObject]] = promoteUserPackageRepo.listUserPromotePackage(page, limit)

  override def deleteUserPromotePackageById(id: String): Future[WriteResult] = promoteUserPackageRepo.deleteUserPromotePackageById(id)
}
