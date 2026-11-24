package com.example.logic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
  private val isoRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

  fun todayIso(nowMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMillis
    return formatCalendarIso(cal)
  }

  fun isValidIsoDate(dateStr: String): Boolean {
    if (!isoRegex.matches(dateStr)) return false
    return parseCalendar(dateStr) != null
  }

  fun addDays(dateIso: String, days: Int): String {
    val cal = parseCalendar(dateIso) ?: Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, days)
    return formatCalendarIso(cal)
  }

  fun expandDateRange(startIso: String, endIso: String, maxDays: Int = 62): List<String> {
    val startCal = parseCalendar(startIso) ?: return emptyList()
    val endCal = parseCalendar(endIso) ?: return emptyList()
    if (startCal.after(endCal)) return emptyList()

    val result = mutableListOf<String>()
    val cursor = startCal.clone() as Calendar
    while (!cursor.after(endCal) && result.size < maxDays) {
      result.add(formatCalendarIso(cursor))
      cursor.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
  }

  fun formatReadableDate(dateIso: String): String {
    val cal = parseCalendar(dateIso) ?: return dateIso
    val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    return fmt.format(cal.time)
  }

  fun formatShortDateWithDay(dateIso: String, todayIso: String = todayIso()): String {
    if (dateIso == todayIso) return "Today"
    if (dateIso == addDays(todayIso, 1)) return "Tomorrow"
    if (dateIso == addDays(todayIso, -1)) return "Yesterday"
    val cal = parseCalendar(dateIso) ?: return dateIso
    val fmt = SimpleDateFormat("EEE, dd MMM", Locale.US)
    return fmt.format(cal.time)
  }

  fun formatTimestamp(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Never"
    val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
    return fmt.format(Calendar.getInstance().apply { timeInMillis = millis }.time)
  }

  fun formatBuildDateVersion(year: Int, month1Based: Int, dayOfMonth: Int): String {
    val shortMonths =
      listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val mon = shortMonths[(month1Based - 1).coerceIn(0, 11)]
    return String.format(Locale.US, "%02d. %s. %04d", dayOfMonth.coerceIn(1, 31), mon, year)
  }

  fun formatBuildDateVersion(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return formatBuildDateVersion(
      year = cal.get(Calendar.YEAR),
      month1Based = cal.get(Calendar.MONTH) + 1,
      dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun isValidTime24h(timeStr: String): Boolean {
    val match = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""").matches(timeStr)
    return match
  }

  fun yearMonthOf(dateIso: String): Pair<Int, Int> {
    val cal = parseCalendar(dateIso) ?: Calendar.getInstance()
    return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
  }

  fun shiftYearMonth(year: Int, month1Based: Int, deltaMonths: Int): Pair<Int, Int> {
    val cal =
      Calendar.getInstance().apply {
        set(year, month1Based - 1, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, deltaMonths)
      }
    return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
  }

  fun formatMonthYearTitle(year: Int, month1Based: Int): String {
    val cal =
      Calendar.getInstance().apply {
        set(year, month1Based - 1, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
  }

  fun buildCalendarMonthGrid(year: Int, month1Based: Int): List<Triple<String, Int, Boolean>> {
    val firstOfMonth =
      Calendar.getInstance().apply {
        set(year, month1Based - 1, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val leadingDays = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = if (leadingDays + daysInMonth > 35) 42 else 35

    val cursor =
      (firstOfMonth.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -leadingDays) }
    val cells = ArrayList<Triple<String, Int, Boolean>>(totalCells)
    for (i in 0 until totalCells) {
      val iso = formatCalendarIso(cursor)
      val dom = cursor.get(Calendar.DAY_OF_MONTH)
      val inMonth =
        cursor.get(Calendar.YEAR) == year && (cursor.get(Calendar.MONTH) + 1) == month1Based
      cells.add(Triple(iso, dom, inMonth))
      cursor.add(Calendar.DAY_OF_MONTH, 1)
    }
    return cells
  }

  fun isoToUtcMillis(dateIso: String): Long? {
    val cal = parseCalendar(dateIso) ?: return null
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
      0,
      0,
      0,
    )
    return utc.timeInMillis
  }

  fun utcMillisToIso(millis: Long): String {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = millis
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      utc.get(Calendar.YEAR),
      utc.get(Calendar.MONTH) + 1,
      utc.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun normalizeDateRange(startIso: String?, endIso: String?): Pair<String?, String?> {
    val validStart = startIso?.takeIf { isValidIsoDate(it) }
    val validEnd = endIso?.takeIf { isValidIsoDate(it) }
    return if (validStart != null && validEnd != null && validStart > validEnd) {
      validEnd to validStart
    } else {
      validStart to validEnd
    }
  }

  private fun parseCalendar(dateIso: String): Calendar? {
    if (!isoRegex.matches(dateIso)) return null
    val parts = dateIso.split("-")
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return try {
      val cal = Calendar.getInstance()
      cal.isLenient = false
      cal.set(year, month - 1, day, 0, 0, 0)
      cal.set(Calendar.MILLISECOND, 0)
      cal.time // triggers strict validation
      cal
    } catch (_: Exception) {
      null
    }
  }

  private fun formatCalendarIso(cal: Calendar): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(cal.time)
  }
}

