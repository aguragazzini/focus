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
 * Personal rows come from this user. Trabajo rows come only from the
 * cross-profile instances URI, and only when that query returns calendars.
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
        val personal = AgendaDay.select(
            readSide(context, window, AgendaSide.PERSONAL).orEmpty(),
            day,
            zone,
        )
        val probed = if (!hasWorkProfile) {
            WorkCalendarAccess.ABSENT
        } else if (enterpriseCalendarsReadable(context)) {
            WorkCalendarAccess.READABLE
        } else {
            WorkCalendarAccess.UNREADABLE
        }
        val workRows = if (probed == WorkCalendarAccess.READABLE) {
            readSide(context, window, AgendaSide.WORK)
        } else {
            emptyList()
        }
        val workAccess = if (probed == WorkCalendarAccess.READABLE && workRows == null) {
            WorkCalendarAccess.UNREADABLE
        } else {
            probed
        }
        val work = if (workAccess == WorkCalendarAccess.READABLE && workRows != null) {
            AgendaDay.select(workRows, day, zone)
        } else {
            emptyList()
        }
        return AgendaSnapshot(
            day = day,
            granted = true,
            personal = personal,
            work = work,
            workAccess = workAccess,
        )
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
        if (id < 0 || begin < 0 || end < 0) return emptyList()
        val out = ArrayList<AgendaRow>()
        while (cursor.moveToNext()) {
            if (visible >= 0 && !cursor.isNull(visible) && cursor.getInt(visible) == 0) continue
            out += AgendaRow(
                eventId = cursor.getLong(id),
                title = if (title >= 0 && !cursor.isNull(title)) cursor.getString(title).orEmpty() else "",
                calendarName = if (name >= 0 && !cursor.isNull(name)) cursor.getString(name).orEmpty() else "",
                beginMillis = cursor.getLong(begin),
                endMillis = cursor.getLong(end),
                allDay = allDay >= 0 && !cursor.isNull(allDay) && cursor.getInt(allDay) == 1,
                color = if (color >= 0 && !cursor.isNull(color)) cursor.getInt(color) else 0,
                status = if (status >= 0 && !cursor.isNull(status)) cursor.getInt(status) else null,
                side = side,
            )
        }
        return out
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
