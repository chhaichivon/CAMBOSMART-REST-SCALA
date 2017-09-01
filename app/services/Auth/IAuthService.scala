package services.Auth

import com.google.inject.{ ImplementedBy, Inject }
import models.promoted.PromoteUser
import models.store.Store
import models.users.User
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import repositories.Auth.IAuthRepo

import scala.concurrent.Future

@ImplementedBy(classOf[AuthService])
trait IAuthService {

  def insertUser(user: User): Future[WriteResult]

  def insertStore(store: Store): Future[WriteResult]

  def getUserByName(name: String): Future[List[User]]

  def getUserByEmail(email: String): Future[Option[User]]

  def getUserByPhone(phone: String): Future[Option[User]]

  def getUserBySocialId(socialId: String): Future[Option[User]]

  def getUserByToken(token: String): Future[Option[User]]

  def getUserById(id: String): Future[Option[User]]

  def getUserByCode(code: String): Future[Option[User]]

  def activeUserByCode(code: String): Future[WriteResult]

  def deleteUserById(id: String): Future[WriteResult]

  def updateUserBySocialId(socialId: String, phone: String, verifiedCode: String): Future[WriteResult]

  def randomCode(): String

  def updateCodeByEmail(verifiedCode: String, email: String): Future[WriteResult]

  def updateCodeByPhone(verifiedCode: String, phone: String): Future[WriteResult]

  def updateCodeByContact(verifiedCode: String, contact: String): Future[WriteResult]

  def updatePasswordByCode(password: String, verifiedCode: String): Future[WriteResult]

  def getAllPhoneOrEmail: Future[List[JsObject]]

}

class AuthService @Inject() (authRepo: IAuthRepo) extends IAuthService {

  override def insertUser(user: User): Future[WriteResult] = authRepo.insertUser(user)

  override def insertStore(store: Store): Future[WriteResult] = authRepo.insertStore(store)

  override def getUserByName(name: String): Future[List[User]] = authRepo.getUserByName(name)

  override def getUserByEmail(email: String): Future[Option[User]] = authRepo.getUserByEmail(email)

  override def getUserByPhone(phone: String): Future[Option[User]] = authRepo.getUserByPhone(phone)

  override def getUserBySocialId(socialId: String): Future[Option[User]] = authRepo.getUserBySocialId(socialId)

  override def getUserByToken(token: String): Future[Option[User]] = authRepo.getUserByToken(token)

  override def getUserById(id: String): Future[Option[User]] = authRepo.getUserById(id)

  override def getUserByCode(code: String): Future[Option[User]] = authRepo.getUserByCode(code)

  override def activeUserByCode(code: String): Future[WriteResult] = authRepo.activeUserByCode(code)

  override def deleteUserById(id: String): Future[WriteResult] = authRepo.deleteUserById(id)

  override def updateUserBySocialId(socialId: String, phone: String, verifiedCode: String): Future[WriteResult] = {
    authRepo.updateUserBySocialId(socialId, phone, verifiedCode)
  }

  override def randomCode(): String = authRepo.randomCode()

  override def updateCodeByEmail(verifiedCode: String, email: String): Future[WriteResult] = authRepo.updateCodeByEmail(verifiedCode, email)

  override def updateCodeByPhone(verifiedCode: String, phone: String): Future[WriteResult] = authRepo.updateCodeByPhone(verifiedCode, phone)

  override def updateCodeByContact(verifiedCode: String, contact: String): Future[WriteResult] = authRepo.updateCodeByContact(verifiedCode, contact)

  override def updatePasswordByCode(password: String, verifiedCode: String): Future[WriteResult] = authRepo.updatePasswordByCode(password, verifiedCode)

  override def getAllPhoneOrEmail: Future[List[JsObject]] = authRepo.getAllPhoneOrEmail

}

