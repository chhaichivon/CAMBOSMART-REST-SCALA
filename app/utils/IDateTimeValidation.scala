package utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
/**
 * Created by CHHAI CHIVON
 */
trait IDateTimeValidation {
  def checkCurrentDate(): Boolean
}
class DateTimeValidation extends IDateTimeValidation {

  override def checkCurrentDate(): Boolean = {
    val date = new Date
    val today = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("mm")
    val hourFormat = new SimpleDateFormat("hh")
    val amPmFormat = new SimpleDateFormat("a")

    val currentHour = hourFormat.format(today) // 12
    val currentMinute = minuteFormat.format(today) // 29
    val amOrPm = amPmFormat.format(today)

    val cal = Calendar.getInstance();
    val format1 = new SimpleDateFormat("MM-dd-yyyy");

    return true
  }
}