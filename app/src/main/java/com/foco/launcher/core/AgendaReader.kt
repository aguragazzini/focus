package com.foco.launcher.core

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import com.foco.launcher.BuildConfig
import java.time.ZoneId

/**
 * Reads today's instances from the calendar provider.
 * Personal rows come from this user. Trabajo keeps the cross-profile
 * instances URI when that query returns calendars, and also events whose
 * personal calendar looks like work. Nothing is invented when both are empty.
 */
object AgendaReader {
    private const val TAG = "AgendaReader"

    fun load(
        context: Context,
        epochMillis: Long,
        zone: ZoneId,
        hasWorkProfile: Boolean,
    ): AgendaSnapshot {
        val day = AgendaDay.dayOf(epochMillis, zone)
        if (!AgendaAccess.granted(context)) {
            return AgendaSnapshot(
                day = day,
                granted = false,
                personal = emptyList(),
                work = emptyList(),
                workAccess = if (hasWorkProfile) WorkCalendarAccess.UNREADABLE else WorkCalendarAccess.ABSENT,
            )
        }
        val window = AgendaDay.queryWindow(day, zone)
        val classified = classifyPersonal(context, readSide(context, window, AgendaSide.PERSONAL).orEmpty())
        val personalOnly = classified.filter { it.side == AgendaSide.PERSONAL }
        val personalWork = classified.filter { it.side == AgendaSide.WORK }
        val enterpriseOk = hasWorkProfile && enterpriseCalendarsReadable(context)
        val enterpriseRows = if (enterpriseOk) readSide(context, window, AgendaSide.WORK) else null
        val enterpriseReadable = enterpriseOk && enterpriseRows != null
        val merged = when {
            enterpriseReadable -> AgendaDay.mergeWork(enterpriseRows.orEmpty(), personalWork)
            personalWork.isNotEmpty() -> personalWork
            else -> emptyList()
        }
        val workAccess = when {
            enterpriseReadable || personalWork.isNotEmpty() -> WorkCalendarAccess.READABLE
            hasWorkProfile -> WorkCalendarAccess.UNREADABLE
            else -> WorkCalendarAccess.ABSENT
        }
        return AgendaSnapshot(
            day = day,
            granted = true,
            personal = AgendaDay.select(personalOnly, day, zone),
            work = if (workAccess == WorkCalendarAccess.READABLE) AgendaDay.select(merged, day, zone) else emptyList(),
            workAccess = workAccess,
            workProfileIsolated = hasWorkProfile && !enterpriseReadable && workAccess == WorkCalendarAccess.READABLE,
        )
    }

    private data class CalendarMeta(
        val name: String,
        val account: String,
        val type: String,
        val visible: Boolean,
    )

    private fun classifyPersonal(context: Context, rows: List<AgendaRow>): List<AgendaRow> {
        if (rows.isEmpty()) return rows
        val metas = readCalendarIndex(context)
        if (metas.isEmpty()) return rows
        return rows.map { row ->
            val meta = metas[row.calendarId]
            val name = meta?.name?.takeIf { it.isNotBlank() } ?: row.calendarName
            val work = meta != null && meta.visible &&
                AgendaDay.looksLikeWork(meta.name, meta.account, meta.type)
            if (work) row.copy(side = AgendaSide.WORK, calendarName = name) else row.copy(calendarName = name)
        }
    }

    private fun readCalendarIndex(context: Context): Map<Long, CalendarMeta> {
        return runCatching {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                calendarProjection,
                null,
                null,
                null,
            ) ?: return emptyMap()
            cursor.use { readCalendars(it) }
        }.getOrElse { error ->
            if (BuildConfig.DEBUG) Log.w(TAG, "calendars", error)
            emptyMap()
        }
    }

    private fun readCalendars(cursor: Cursor): Map<Long, CalendarMeta> {
        val id = cursor.getColumnIndex(CalendarContract.Calendars._ID)
        val name = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val account = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
        val type = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
        val visible = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
        if (id < 0) return emptyMap()
        val out = HashMap<Long, CalendarMeta>()
        while (cursor.moveToNext()) {
            val shown = visible < 0 || cursor.isNull(visible) || cursor.getInt(visible) != 0
            out[cursor.getLong(id)] = CalendarMeta(
                name = text(cursor, name),
                account = text(cursor, account),
                type = text(cursor, type),
                visible = shown,
            )
        }
        return out
    }

    /** Null means the provider rejected the query. An empty list is a real empty result. */
    private fun readSide(context: Context, window: LongRange, side: AgendaSide): List<AgendaRow>? {
        return runCatching {
            val cursor = if (side == AgendaSide.PERSONAL) {
                CalendarContract.Instances.query(
                    context.contentResolver,
                    personalProjection,
                    window.first,
                    window.last,
                )
            } else {
                val uri = ranged(CalendarContract.Instances.ENTERPRISE_CONTENT_URI, window)
                context.contentResolver.query(uri, workProjection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
            } ?: return emptyList()
            cursor.use { readCursor(it, side) }
        }.getOrElse { error ->
            if (BuildConfig.DEBUG) Log.w(TAG, "instances $side", error)
            null
        }
    }

    private fun enterpriseCalendarsReadable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return runCatching {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.ENTERPRISE_CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                null,
                null,
                null,
            ) ?: return false
            cursor.use { it.moveToFirst() }
        }.getOrElse { error ->
            if (BuildConfig.DEBUG) Log.w(TAG, "enterprise calendars", error)
            false
        }
    }

    private fun readCursor(cursor: Cursor, side: AgendaSide): List<AgendaRow> {
        val id = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
        val title = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
        val begin = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
        val end = cursor.getColumnIndex(CalendarContract.Instances.END)
        val allDay = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
        val color = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
        val status = cursor.getColumnIndex(CalendarContract.Instances.STATUS)
        val visible = cursor.getColumnIndex(CalendarContract.Instances.VISIBLE)
        val name = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
        val calendarId = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
        if (id < 0 || begin < 0 || end < 0) return emptyList()
        val out = ArrayList<AgendaRow>()
        while (cursor.moveToNext()) {
            if (visible >= 0 && !cursor.isNull(visible) && cursor.getInt(visible) == 0) continue
            out += AgendaRow(
                eventId = cursor.getLong(id),
                title = text(cursor, title),
                calendarName = text(cursor, name),
                beginMillis = cursor.getLong(begin),
                endMillis = cursor.getLong(end),
                allDay = allDay >= 0 && !cursor.isNull(allDay) && cursor.getInt(allDay) == 1,
                color = if (color >= 0 && !cursor.isNull(color)) cursor.getInt(color) else 0,
                status = if (status >= 0 && !cursor.isNull(status)) cursor.getInt(status) else null,
                side = side,
                calendarId = if (calendarId >= 0 && !cursor.isNull(calendarId)) cursor.getLong(calendarId) else 0L,
            )
        }
        return out
    }

    private fun text(cursor: Cursor, index: Int): String {
        return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index).orEmpty() else ""
    }

    private fun ranged(base: Uri, window: LongRange): Uri {
        return base.buildUpon()
            .appendPath(window.first.toString())
            .appendPath(window.last.toString())
            .build()
    }

    private val personalProjection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.STATUS,
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        CalendarContract.Instances.CALENDAR_ID,
    )

    private val calendarProjection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE,
        CalendarContract.Calendars.VISIBLE,
    )

    private val workProjection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.STATUS,
        CalendarContract.Instances.VISIBLE,
    )
}
