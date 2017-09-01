package services.User

import com.google.inject.{ ImplementedBy, Inject }
import models.promoted.PromoteUser
import models.users.User
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.User.IUserRepo

import scala.concurrent.Future

@ImplementedBy(classOf[UserService])
trait IUserService {
  /*chivon*/
  def getFilterMembers(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]]
  def updateMember(id: BSONObjectID, user: User): Future[Boolean]
  def deleteMember(id: BSONObjectID): Future[Boolean]
  def getUserWithStoreInfoService(username: String): Future[List[JsObject]]

  /* sona */
  def getMemberById(id: String): Future[Option[User]]
  def blockMemberById(id: String, status: Int): Future[WriteResult]

  /*naseat*/
  def blockMerchantById(id: String): Future[WriteResult]
  def getMerchantById(id: String): Future[Option[User]]

  /* upload image */
  def uploadImage(id: String, image: String): Future[WriteResult]

  /*update profile*/
  def updateMemberProfile(id: String, userName: String, phone: String, email: String, phones: List[String], location: String, address: String): Future[WriteResult]

  def changePassword(id: String, newPassword: String): Future[WriteResult]

  def countAllNormalMembers(): Future[Int]
  def countAllMerchantMembers(): Future[Int]
}

class UserService @Inject() (val userRepo: IUserRepo) extends IUserService {

  /*chivon*/
  override def getFilterMembers(userType: String, name: String, location: String, status: Int, fromDate: String, toDate: String, start: Int, limit: Int): Future[List[JsObject]] = {
    userRepo.getFilterMembers(userType, name, location, status, fromDate, toDate, start, limit)
  }
  override def updateMember(id: BSONObjectID, user: User): Future[Boolean] = {
    userRepo.updateMember(id, user)
  }
  override def deleteMember(id: BSONObjectID): Future[Boolean] = {
    userRepo.deleteMember(id)
  }
  override def getUserWithStoreInfoService(username: String): Future[List[JsObject]] = {
    userRepo.getUserWithStoreInfoRepo(username)
  }

  /* sona */
  override def getMemberById(id: String): Future[Option[User]] = userRepo.getMemberById(id)
  override def blockMemberById(id: String, status: Int): Future[WriteResult] = userRepo.blockMemberById(id, status)

  /*naseat*/
  override def blockMerchantById(id: String): Future[WriteResult] = userRepo.blockMerchantById(id)
  override def getMerchantById(id: String): Future[Option[User]] = userRepo.getMerchantById(id)

  /* upload image */
  override def uploadImage(id: String, image: String): Future[WriteResult] = userRepo.uploadImage(id, image)

  /*update profile*/
  override def updateMemberProfile(id: String, userName: String, phone: String, email: String, phones: List[String], location: String, address: String): Future[WriteResult] = {
    userRepo.updateMemberProfile(id, userName, phone, email, phones, location, address)
  }

  override def changePassword(id: String, newPassword: String): Future[WriteResult] = userRepo.changePassword(id, newPassword)

  override def countAllNormalMembers(): Future[Int] = userRepo.countAllNormalMembers()

  override def countAllMerchantMembers(): Future[Int] = userRepo.countAllMerchantMembers()
}