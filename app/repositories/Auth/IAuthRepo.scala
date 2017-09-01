package repositories.Auth

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.store.Store
import models.users.User
import play.api.libs.json.{ JsObject, Json }
import play.api.libs.json.Json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Random, Success }

@ImplementedBy(classOf[AuthRepo])
trait IAuthRepo {

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

class AuthRepo @Inject() (reactiveMongoApi: ReactiveMongoApi) extends IAuthRepo {

  def collection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("user_tbl"))
  def storeCollection = reactiveMongoApi.database.map(db => db.collection[JSONCollection]("store_tbl"))

  override def insertUser(user: User): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.insert(user))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new user with result : $writeResult")
    }
    writeRes
  }

  override def insertStore(store: Store): Future[WriteResult] = {
    val writeRes = storeCollection.flatMap(_.insert(store))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successful insert new store with result : $writeResult")
    }
    writeRes
  }

  override def getUserByName(name: String): Future[List[User]] = {
    import reactivemongo.play.json.collection.JSONBatchCommands.AggregationFramework._
    val user = collection.flatMap(_.aggregate(
      Project(
        Json.obj(
          "mistakeId" -> 1,
          "socialId" -> 1,
          "userName" -> 1,
          "username" -> Json.obj("$toLower" -> "$userName"),
          "phone" -> 1,
          "email" -> 1,
          "password" -> 1,
          "verifiedCode" -> 1,
          "otherPhones" -> 1,
          "city" -> 1,
          "address" -> 1,
          "userType" -> 1,
          "dateJoin" -> 1,
          "dateBlock" -> 1,
          "profileImage" -> 1,
          "status" -> 1,
          "online" -> 1
        )
      ),
      List(
        Match(Json.obj("username" -> name.toLowerCase))
      )
    ).map(_.head[User]))

    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful find user by username !!")
    }
    user
  }

  override def getUserByEmail(email: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("email" -> email)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful find user by email !!")
    }
    user
  }

  override def getUserByPhone(phone: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("phone" -> phone)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful find user by phone !!")
    }
    user
  }

  override def getUserBySocialId(socialId: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("socialId" -> socialId)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful find user by socialId !!")
    }
    user
  }

  override def getUserByToken(token: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("token" -> token)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful find user by token !!")
    }
    user
  }

  override def getUserById(id: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("_id" -> BSONObjectID.parse(id).get)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully find user with result: $data")
    }
    user
  }

  override def getUserByCode(code: String): Future[Option[User]] = {
    val user = collection.flatMap(_.find(obj("verifiedCode" -> code)).one[User])
    user.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successfully find user by code !!")
    }
    user
  }

  override def activeUserByCode(code: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("verifiedCode" -> code),
      BSONDocument(
        "$set" -> BSONDocument(
          "status" -> 1,
          "online" -> 1
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully activate account with result: $writeResult")
    }
    writeRes
  }

  override def deleteUserById(id: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.remove(obj("_id" -> BSONObjectID.parse(id).get)))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully delete user with result: $writeResult")
    }
    writeRes
  }

  override def updateUserBySocialId(socialId: String, phone: String, verifiedCode: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("socialId" -> socialId),
      BSONDocument(
        "$set" -> BSONDocument(
          "phone" -> phone,
          "verifiedCode" -> verifiedCode
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully update account with result: $writeResult")
    }
    writeRes
  }

  override def randomCode(): String = {
    def newCode: String = {
      val code = (100000 + Random.nextInt(900000)).toString
      val user = collection.flatMap(_.find(obj())
        .cursor[User](ReadPreference.primary)
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[User]]()))
      user.onComplete {
        case Failure(e) => e.printStackTrace()
        case Success(data) => println(s"Successful find users !!")
      }
      Await.result(user.map(usr => if (!usr.exists(_.verifiedCode == code)) code else newCode), 0.5.seconds)
    }
    newCode
  }

  override def updateCodeByEmail(verifiedCode: String, email: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("email" -> email),
      BSONDocument(
        "$set" -> BSONDocument(
          "verifiedCode" -> verifiedCode
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully update verifiedCode by email with result: $writeResult")
    }
    writeRes
  }

  override def updateCodeByPhone(verifiedCode: String, phone: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("phone" -> phone),
      BSONDocument(
        "$set" -> BSONDocument(
          "verifiedCode" -> verifiedCode
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully update verifiedCode by phone number with result: $writeResult")
    }
    writeRes
  }

  override def updateCodeByContact(verifiedCode: String, contact: String): Future[WriteResult] = {
    val reg = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b""".r
    var filter = Json.obj()
    reg findFirstIn contact match {
      case Some(email) =>
        println("Match: " + email)
        filter = Json.obj("email" -> email)
      case None =>
        println("Fail : " + contact)
        filter = Json.obj("phone" -> contact)
    }

    val writeRes = collection.flatMap(_.update(
      filter,
      BSONDocument(
        "$set" -> BSONDocument(
          "verifiedCode" -> verifiedCode
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully update verifiedCode by contact $contact with result: $writeResult")
    }
    writeRes
  }

  override def updatePasswordByCode(password: String, verifiedCode: String): Future[WriteResult] = {
    val writeRes = collection.flatMap(_.update(
      Json.obj("verifiedCode" -> verifiedCode),
      BSONDocument(
        "$set" -> BSONDocument(
          "password" -> password
        )
      )
    ))
    writeRes.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) => println(s"Successfully update password by verifyCode with result: $writeResult")
    }
    writeRes
  }

  override def getAllPhoneOrEmail: Future[List[JsObject]] = {
    val users = collection.flatMap(_.find(obj())
      .projection(Json.obj("_id" -> 0, "userName" -> 1, "phone" -> 1, "email" -> 1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JsObject]]()))
    users.onComplete {
      case Failure(e) => e.printStackTrace()
      case Success(data) => println(s"Successful get all userNames/phones/emails !!")
    }
    users
  }

}

