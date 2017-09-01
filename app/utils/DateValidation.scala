package utils

import api.Api
import org.joda.time.DateTime

import scala.util.{ Failure, Success, Try }

object DateValidation {

  def validate(date: String): Boolean = {
    val checkDate = Try[DateTime](Api.parseHeaderDate(date))
    checkDate match {
      case Success(d) =>
        println("Date check success : ", d); true
      case Failure(e) => e.printStackTrace(); true
    }
  }
}
