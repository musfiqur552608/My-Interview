package com.freedu.myinterviews.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private fun fmt(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.getDefault())

    fun dateTime(millis: Long): String = fmt("EEE, d MMM · h:mm a").format(Date(millis))
    fun date(millis: Long): String = fmt("d MMM yyyy").format(Date(millis))
    fun time(millis: Long): String = fmt("h:mm a").format(Date(millis))

    fun relativeDay(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = millis - now
        if (diff < 0) return dateTime(millis)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        return when {
            days == 0L && hours == 0L -> "in less than an hour"
            days == 0L -> "in $hours hr"
            days == 1L -> "tomorrow"
            days < 7 -> "in $days days"
            else -> dateTime(millis)
        }
    }

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun countdown(deadline: Long, now: Long = System.currentTimeMillis()): String {
        if (deadline <= 0) return "No deadline"
        val d = TimeUnit.MILLISECONDS.toDays(deadline - now)
        if (deadline < now) return "Deadline passed"
        if (d == 0L) return "Due today"
        if (d == 1L) return "1 day left"
        return "$d days left"
    }
}
