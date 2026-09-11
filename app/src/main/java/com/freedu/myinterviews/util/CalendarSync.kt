package com.freedu.myinterviews.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.freedu.myinterviews.domain.model.InterviewRound
import java.util.TimeZone

/**
 * Optional two-way-light sync of interview rounds into the device calendar
 * (CalendarContract). Off by default; enable in Settings.
 *
 * Mapping round → calendar event id is kept in private SharedPreferences
 * (no schema change needed). Past/cancelled/non-upcoming rounds are removed.
 */
object CalendarSync {
    private const val PREFS = "calendar_sync"
    private const val KEY_ENABLED = "enabled"
    private const val ACCOUNT = "InterviewTrackerLocal"
    private const val CAL_NAME = "Interview Tracker"

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun syncUri(base: Uri): Uri = base.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    /** Find or create the local "Interview Tracker" calendar. Null if not permitted/failing. */
    fun ensureCalendar(ctx: Context): Long? {
        if (!hasPermission(ctx)) return null
        val cr = ctx.contentResolver
        runCatching {
            cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                "${CalendarContract.Calendars.ACCOUNT_NAME}=? AND ${CalendarContract.Calendars.ACCOUNT_TYPE}=?",
                arrayOf(ACCOUNT, CalendarContract.ACCOUNT_TYPE_LOCAL), null
            )?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
        }
        return runCatching {
            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(CalendarContract.Calendars.NAME, CAL_NAME)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CAL_NAME)
                put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF6750A4.toInt())
                // 700 = CALENDAR_ACCESS_OWNER (constant lives on the protected
                // CalendarColumns interface in this SDK — value is API-stable).
                put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, 700)
                put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                put(CalendarContract.Calendars.VISIBLE, 1)
            }
            val uri = cr.insert(syncUri(CalendarContract.Calendars.CONTENT_URI), values)
                ?: return null
            ContentUris.parseId(uri)
        }.getOrNull()
    }

    private fun storedEvent(ctx: Context, roundId: Long): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("ev_$roundId", 0)

    private fun storeEvent(ctx: Context, roundId: Long, eventId: Long) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("ev_$roundId", eventId).apply()
    }

    private fun forgetEvent(ctx: Context, roundId: Long) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("ev_$roundId").apply()
    }

    /** Insert or refresh the event for a round; removes it when not upcoming. Returns event id or null. */
    fun upsertEvent(ctx: Context, round: InterviewRound): Long? {
        if (!isEnabled(ctx) || !hasPermission(ctx)) return null
        if (round.status.name != "UPCOMING") {
            deleteEvent(ctx, round.id)
            return null
        }
        val calId = ensureCalendar(ctx) ?: return null
        val cr = ctx.contentResolver
        return runCatching {
            deleteEvent(ctx, round.id) // replace-instead-of-update: simple + idempotent
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, "Interview: ${round.jobTitle.ifBlank { "upcoming round" }} @ ${round.companyName}")
                put(
                    CalendarContract.Events.DESCRIPTION,
                    buildString {
                        append("${round.roundType.name.lowercase().replace('_', ' ')} · ${round.mode}")
                        if (round.interviewers.isNotBlank()) append("\nWith: ${round.interviewers}")
                        if (round.platformLink.isNotBlank()) append("\nLink: ${round.platformLink}")
                        append("\n\nTracked in Interview Tracker")
                    }
                )
                put(CalendarContract.Events.DTSTART, round.scheduledAt)
                put(CalendarContract.Events.DURATION, "PT${round.durationMin.coerceAtLeast(15)}M")
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
            val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
            val eventId = ContentUris.parseId(uri)
            // 1-hour + 1-day alarms.
            listOf(60, 1440).forEach { mins ->
                cr.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.MINUTES, mins)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                )
            }
            storeEvent(ctx, round.id, eventId)
            eventId
        }.getOrNull()
    }

    fun deleteEvent(ctx: Context, roundId: Long) {
        val eventId = storedEvent(ctx, roundId)
        if (eventId == 0L) return
        forgetEvent(ctx, roundId)
        if (!hasPermission(ctx)) return
        runCatching {
            ctx.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null, null
            )
        }
    }

    /** Remove every event this app created (used when the toggle is switched off). */
    fun deleteAll(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.all.keys.filter { it.startsWith("ev_") }
        ids.forEach { key ->
            val eventId = prefs.getLong(key, 0)
            prefs.edit().remove(key).apply()
            if (eventId != 0L && hasPermission(ctx)) {
                runCatching {
                    ctx.contentResolver.delete(
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                        null, null
                    )
                }
            }
        }
    }
}
