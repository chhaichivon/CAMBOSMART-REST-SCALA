package utils

import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message

object SMS {
  val ACCOUNT_SID = "AC6c2c04dcbab02298f6b52a98e8c2e501" //"AC69a79e10d6d0d14567e2dbd06d4c7b00"
  val AUTH_TOKEN = "ef57943609c6ee92982796afa633ad76" //"adb760a01a5ee9365fce7c303b0e47a1"

  def SendSMS(phone: String, code: String) = {
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN)
    try {
      Message
        .creator(
          new PhoneNumber(phone), // to
          new PhoneNumber("+13478975782"), // from +12408396307
          "Welcome to www.cambosmart.com \n" +
            "Here is you verification code : " + code
        )
        .create()
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}