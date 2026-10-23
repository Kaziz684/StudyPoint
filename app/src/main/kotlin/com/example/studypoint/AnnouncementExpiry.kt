package com.example.studypoint

import java.util.Calendar
import java.util.Locale

/**
 * Auto-expiry for board announcements, so stale news disappears from the
 * home card and the bell list on its own:
 *
 * - An item lives until its date ends ("date khatam = announcement khatam").
 *   A day-dated item (e.g. a result on 22 Oct) therefore shows for that day
 *   only, roughly 24 hours. A month-dated item (e.g. "Oct 2026") lives till
 *   the end of that month.
 * - If the title/description mentions a date RANGE (e.g. practicals
 *   "30 Oct – 17 Nov", or "12th 3–16 Nov, 11th 17–28 Nov, Practicals 1–11 Dec"),
 *   the item lives until the LATEST mentioned date, because the date field
 *   itself usually holds only the start of the range.
 * - Items whose date cannot be parsed are kept (never hide content we do
 *   not understand).
 */
object AnnouncementExpiry {

    private val months = mapOf(
        "jan" to 0, "january" to 0,
        "feb" to 1, "february" to 1,
        "mar" to 2, "march" to 2,
        "apr" to 3, "april" to 3,
        "may" to 4,
        "jun" to 5, "june" to 5,
        "jul" to 6, "july" to 6,
        "aug" to 7, "august" to 7,
        "sep" to 8, "sept" to 8, "september" to 8,
        "oct" to 9, "october" to 9,
        "nov" to 10, "november" to 10,
        "dec" to 11, "december" to 11
    )

    private val fullDateRegex = Regex("""(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})""")
    private val monthYearRegex = Regex("""([A-Za-z]+)\s+(\d{4})""")
    private val dayMonthRegex = Regex("""(\d{1,2})\s+([A-Za-z]+)""")

    /** "30 Oct 2026" -> (30, 9, 2026); "Oct 2026" -> (null, 9, 2026); else null. */
    private fun parseDate(text: String): Triple<Int?, Int, Int>? {
        val full = fullDateRegex.find(text)
        if (full != null) {
            val month = months[full.groupValues[2].lowercase(Locale.ENGLISH)] ?: return null
            return Triple(full.groupValues[1].toInt(), month, full.groupValues[3].toInt())
        }
        val monthYear = monthYearRegex.find(text)
        if (monthYear != null) {
            val month = months[monthYear.groupValues[1].lowercase(Locale.ENGLISH)] ?: return null
            return Triple(null, month, monthYear.groupValues[2].toInt())
        }
        return null
    }

    private fun endOfDayMillis(day: Int, month: Int, year: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, 23, 59, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun endOfMonthMillis(month: Int, year: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month, 1)
        val last = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        return endOfDayMillis(last, month, year)
    }

    /** Latest day-month mentioned in title/description, resolved against [baseYear]. */
    private fun latestMentionedEnd(item: BoardAnnouncement, baseYear: Int): Long? {
        val text = "${item.title} ${item.description}"
        var latest: Long? = null
        dayMonthRegex.findAll(text).forEach { m ->
            val month = months[m.groupValues[2].lowercase(Locale.ENGLISH)] ?: return@forEach
            val t = endOfDayMillis(m.groupValues[1].toInt(), month, baseYear)
            if (latest == null || t > latest!!) latest = t
        }
        return latest
    }

    /** Millis after which the item must stop showing, or null if undetermined. */
    fun expiryMillis(item: BoardAnnouncement): Long? {
        val parsed = parseDate(item.date) ?: return null
        val (day, month, year) = parsed
        val fieldEnd = if (day != null) endOfDayMillis(day, month, year)
            else endOfMonthMillis(month, year)
        val rangeEnd = latestMentionedEnd(item, year)
        return if (rangeEnd != null) maxOf(fieldEnd, rangeEnd) else fieldEnd
    }

    fun isExpired(item: BoardAnnouncement, nowMs: Long = System.currentTimeMillis()): Boolean {
        val expiry = expiryMillis(item) ?: return false
        return nowMs > expiry
    }

    fun filterUnexpired(items: List<BoardAnnouncement>): List<BoardAnnouncement> =
        items.filter { !isExpired(it) }

    /** Newest first by the date field (month-only counts as the 1st of that month). */
    fun sortNewestFirst(items: List<BoardAnnouncement>): List<BoardAnnouncement> {
        fun sortKey(item: BoardAnnouncement): Long {
            val parsed = parseDate(item.date) ?: return 0L
            val (day, month, year) = parsed
            val c = Calendar.getInstance()
            c.set(year, month, day ?: 1, 0, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
        return items.sortedByDescending { sortKey(it) }
    }
}
