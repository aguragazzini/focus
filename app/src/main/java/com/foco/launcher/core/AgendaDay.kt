package com.foco.launcher.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/**
 * Today's events in the civil zone. [select] keeps the side already on the row.
 * A calendar is moved to Trabajo only by [looksLikeWork], before select.
 */
enum class AgendaSide {
    PERSONAL,
    WORK,
}

enum class WorkCalendarAccess {
    /** No managed profile on this phone. */
    ABSENT,
    /** Profile exists, but this process cannot read its calendars. */
    UNREADABLE,
    /** The cross-profile calendar query returned at least one calendar. */
    READABLE,
}

data class AgendaRow(
    val eventId: Long,
    val title: String,
    val calendarName: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Int,
    val status: Int?,
    val side: AgendaSide,
    val calendarId: Long = 0,
)

data class AgendaEvent(
    val eventId: Long,
    val title: String,
    val calendarName: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Int,
    val side: AgendaSide,
)

data class AgendaSnapshot(
    val day: LocalDate,
    val granted: Boolean,
    val personal: List<AgendaEvent>,
    val work: List<AgendaEvent>,
    val workAccess: WorkCalendarAccess,
    /**
     * The work profile's own calendars were not readable. Trabajo rows, if any,
     * come from calendars in this profile that look like work.
     */
    val workProfileIsolated: Boolean = false,
)

object AgendaDay {
    /** AOSP EventsColumns.STATUS_CANCELED. Passed in so tests do not read the stub jar. */
    const val STATUS_CANCELED = 2

    private val esAr = Locale("es", "AR")
    private val words = Regex("[^\\p{L}\\p{N}]+")
    private val workWords = setOf("work", "trabajo")
    private val workAccountTypes = setOf("work", "exchange", "activesync")

    /**
     * Confident work calendars only. A whole word "work" or "trabajo" in the
     * display name or account name counts. "Homework", "workshop", and "Network"
     * do not. Account types match a segment: exchange, activesync, or work.
     * An email domain is not a guess, and "oficina" is not enough.
     */
    fun looksLikeWork(displayName: String, accountName: String, accountType: String): Boolean {
        if (wordsOf(displayName).any { it in workWords }) return true
        if (wordsOf(accountName).any { it in workWords }) return true
        return wordsOf(accountType).any { it in workAccountTypes }
    }

    /**
     * Enterprise rows win. A personal row with the same title and start is the
     * same event and is dropped, unless it is the only copy that has a calendar name.
     */
    fun mergeWork(enterprise: List<AgendaRow>, personalWork: List<AgendaRow>): List<AgendaRow> {
        val byKey = LinkedHashMap<String, AgendaRow>()
        for (row in enterprise + personalWork) {
            val key = "${row.title.trim().lowercase(esAr)}:${row.beginMillis}:${row.allDay}"
            val existing = byKey[key]
            val stamped = if (row.side == AgendaSide.WORK) row else row.copy(side = AgendaSide.WORK)
            if (existing == null) {
                byKey[key] = stamped
            } else if (existing.calendarName.isBlank() && stamped.calendarName.isNotBlank()) {
                byKey[key] = stamped
            }
        }
        return byKey.values.toList()
    }

    private fun wordsOf(value: String): List<String> {
        return value.lowercase(esAr).split(words).filter { it.isNotEmpty() }
    }

    fun dayOf(epochMillis: Long, zone: ZoneId): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    }

    /**
     * Instances are queried a day past each edge. All-day rows are stored at UTC
     * midnight, which sits before local midnight in Córdoba.
     */
    fun queryWindow(day: LocalDate, zone: ZoneId): LongRange {
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val pad = 24L * 60L * 60L * 1000L
        return (start - pad)..(end + pad)
    }

    fun covers(day: LocalDate, begin: Long, end: Long, allDay: Boolean, zone: ZoneId): Boolean {
        if (allDay) {
            val startDate = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
            if (end <= begin) return startDate == day
            val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()
            return !day.isBefore(startDate) && day.isBefore(endDate)
        }
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val finish = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return begin < finish && end > start
    }

    fun clock(epochMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return "%02d:%02d".format(local.hour, local.minute)
    }

    fun select(
        rows: List<AgendaRow>,
        day: LocalDate,
        zone: ZoneId,
        canceledStatus: Int = STATUS_CANCELED,
    ): List<AgendaEvent> {
        val seen = HashSet<String>()
        val kept = ArrayList<AgendaEvent>()
        for (row in rows) {
            if (row.status == canceledStatus) continue
            if (!covers(day, row.beginMillis, row.endMillis, row.allDay, zone)) continue
            val key = "${row.side}:${row.eventId}:${row.beginMillis}"
            if (!seen.add(key)) continue
            kept += AgendaEvent(
                eventId = row.eventId,
                title = row.title.trim(),
                calendarName = row.calendarName.trim(),
                beginMillis = row.beginMillis,
                endMillis = row.endMillis,
                allDay = row.allDay,
                color = row.color,
                side = row.side,
            )
        }
        kept.sortWith(
            compareBy<AgendaEvent> { if (it.allDay) 0 else 1 }
                .thenBy { it.beginMillis }
                .thenBy { it.title },
        )
        return kept
    }
}
