package utils
import java.net._
import java.io._

object BulkSMS {

  /*def stringToHex(message: String): String = {
    char[] chars = s.toCharArray()
    String next;
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < chars.length; i++) {
      next = Integer.toHexString((int)chars[i]);
      // Unfortunately, toHexString doesn't pad with zeroes, so we have to.
      for (int j = 0; j < (4-next.length()); j++)  {
        output.append("0");
      }
      output.append(next);
    }
    return output.toString();
  }*/

  /*def sendSMS() = {
    try{
      var data = ""
      data += "username=" + URLEncoder.encode("stonkaka", "ISO-8859-1")
      data += "&password=" + URLEncoder.encode("SengoudaM2542016", "ISO-8859-1")
      data += "&message=" + stringToHex("This is a test: 121231 ☺ \nKhmer: សួរស្ដីរ\nChinese: 本网")
      data += "&dca=16bit"
      data += "&want_report=1"
      data += "&msisdn=85570906308"

      // Send data
      // Please see the FAQ regarding HTTPS (port 443) and HTTP (port 80/5567)
      val url: URL = new URL("https://bulksms.vsms.net/eapi/submission/send_sms/2/2.0")
      val conn: URLConnection = url.openConnection()
      conn.setDoOutput(true)
      val wr: OutputStreamWriter = new OutputStreamWriter(conn.getOutputStream)
      wr.write(data)
      wr.flush()
      wr.close()
    }catch {
      case e: Throwable => e.printStackTrace()
    }
  }*/

  def main(args: Array[String]) {
    try {
      /*var data = ""
      data += "username=" + URLEncoder.encode("stonkaka", "ISO-8859-1")
      data += "&password=" + URLEncoder.encode("SengoudaM2542016", "ISO-8859-1")
      data += "&message=" + stringToHex("This is a test: 121231 ☺ \nKhmer: សួរស្ដីរ\nChinese: 本网")
      data += "&dca=16bit"
      data += "&want_report=1"
      data += "&msisdn=85570906308"

      // Send data
      // Please see the FAQ regarding HTTPS (port 443) and HTTP (port 80/5567)
      val url: URL = new URL("https://bulksms.vsms.net/eapi/submission/send_sms/2/2.0")
      val conn: URLConnection = url.openConnection()
      conn.setDoOutput(true)
      val wr: OutputStreamWriter = new OutputStreamWriter(conn.getOutputStream)
      wr.write(data)
      wr.flush()
      wr.close()*/
      print("Hello")
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}