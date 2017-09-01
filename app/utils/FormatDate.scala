package utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object FormatDate {
  private final val longDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  def parseDate(dateStr: String): DateTime = longDateTimeFormatter.parseDateTime(dateStr)
  def printDate(date: DateTime): String = longDateTimeFormatter.print(date)
}
