package com.freedu.myinterviews.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Prefilled draft extracted from pasted recruiter-email text. */
data class EmailDraft(
    val company: String = "",
    val role: String = "",
    val scheduledAt: Long? = null,
    val interviewer: String = "",
    val link: String = "",
    val mode: String = "Video"
)

/**
 * Best-effort recruiter-email parser (fully offline regex heuristics).
 * Paste any scheduling email → get company, role, date/time, interviewer and
 * video link prefilled. Never perfect; the UI always lets the user correct it.
 */
object EmailParser {

    private val LINK = Regex("""https?://\S*(?:zoom\.us|meet\.google\.com|teams\.microsoft\.com|teams\.live\.com|webex\.com|chime\.aws)\S*""")
    private val TIME = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm|AM|PM)?(?:\s*[-–]\s*\d{1,2}(?::\d{2})?\s*(?:am|pm|AM|PM)?)?\s*(CET|CEST|BST|GMT|UTC|EST|EDT|PST|PDT|IST)?""")
    private val MONTH_DAY = Regex("""(?i)(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?""")
    private val DAY_MONTH = Regex("""(?i)\b(\d{1,2})(?:st|nd|rd|th)?\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)""")
    private val WEEKDAY = Regex("""(?i)\b(next\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""")
    private val WITH = Regex("""with\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+){0,2})""")
    private val IAM = Regex("""I(?:'m| am)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+){0,2})""")
    private val ROLE_FOR = Regex("""(?i)for\s+(?:the\s+)?(.{3,60}?)\s+(position|role|opportunity|opening)\b""")
    private val ROLE_COLON = Regex("""(?i)(?:position|role|requisition|job title)\s*[:–-]\s*(.{3,60})""")
    private val AT_COMPANY = Regex("""\bat\s+([A-Z][\w&.,'\-]*(?:\s+[A-Z][\w&.,'\-]*){0,3})""")
    private val FROM_COMPANY = Regex("""(?i)from\s+([A-Z][\w&.,'\-]*(?:\s+[\w&.,'\-]*){0,3})""")

    private val MONTHS = mapOf(
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3, "may" to 4, "jun" to 5,
        "jul" to 6, "aug" to 7, "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11
    )
    private val DAYS = mapOf(
        "sunday" to Calendar.SUNDAY, "monday" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY, "wednesday" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "friday" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY
    )

    fun parse(text: String, now: Long = System.currentTimeMillis()): EmailDraft {
        if (text.isBlank()) return EmailDraft()
        val link = LINK.find(text)?.value?.trimEnd('.', ',', ')').orEmpty()
        val mode = when {
            "zoom" in link -> "Video (Zoom)"
            "meet.google" in link -> "Video (Meet)"
            "teams" in link -> "Video (Teams)"
            text.contains("onsite", true) || text.contains("on-site", true) -> "Onsite"
            text.contains("phone", true) -> "Phone"
            link.isNotEmpty() -> "Video"
            else -> "Video"
        }
        val interviewer = WITH.find(text)?.groupValues?.get(1)?.trim()
            ?: IAM.find(text)?.groupValues?.get(1)?.trim().orEmpty()
        val role = ROLE_COLON.find(text)?.groupValues?.get(1)?.trim()?.take(60)
            ?: ROLE_FOR.find(text)?.groupValues?.get(1)?.trim()?.take(60).orEmpty()
        val company = cleanCompany(
            AT_COMPANY.find(text)?.groupValues?.get(1)?.trim().orEmpty()
                .ifBlank { FROM_COMPANY.find(text)?.groupValues?.get(1)?.trim().orEmpty() }
        )
        return EmailDraft(
            company = company,
            role = role.trim().trimEnd('.', ',', ':'),
            scheduledAt = parseDateTime(text, now),
            interviewer = interviewer,
            link = link,
            mode = mode
        )
    }

    private fun cleanCompany(raw: String): String {
        if (raw.isBlank()) return ""
        // Drop trailing lowercase filler ("at Acme and I" → "Acme")
        val words = raw.split(" ").toMutableList()
        while (words.size > 1 && words.last().first().isLowerCase()) words.removeLast()
        return words.joinToString(" ").trimEnd(',', '.', ':').take(60)
    }

    private fun parseDateTime(text: String, now: Long): Long? {
        val lower = text.lowercase()
        var base: Calendar? = null
        MONTH_DAY.find(text)?.let { m ->
            val month = MONTHS[m.groupValues[1].lowercase().take(3)] ?: return@let
            val day = m.groupValues[2].toIntOrNull() ?: return@let
            val c = Calendar.getInstance().apply { timeInMillis = now }
            c.set(Calendar.MONTH, month); c.set(Calendar.DAY_OF_MONTH, day)
            if (c.timeInMillis < now - 86_400_000L) c.add(Calendar.YEAR, 1)
            base = c
        }
        if (base == null) DAY_MONTH.find(text)?.let { m ->
            val month = MONTHS[m.groupValues[2].lowercase().take(3)] ?: return@let
            val day = m.groupValues[1].toIntOrNull() ?: return@let
            val c = Calendar.getInstance().apply { timeInMillis = now }
            c.set(Calendar.MONTH, month); c.set(Calendar.DAY_OF_MONTH, day)
            if (c.timeInMillis < now - 86_400_000L) c.add(Calendar.YEAR, 1)
            base = c
        }
        if (base == null) WEEKDAY.find(text)?.let { m ->
            val next = m.groupValues[1].isNotBlank()
            val target = DAYS[m.groupValues[2].lowercase()] ?: return@let
            val c = Calendar.getInstance().apply { timeInMillis = now }
            var delta = (target - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (delta == 0) delta = 7 // bare "Monday" on a Monday → the coming one
            if (next) delta += 7 // "next Monday" → the one after
            c.add(Calendar.DAY_OF_YEAR, delta)
            base = c
        }
        if (base == null) {
            if ("tomorrow" in lower) {
                base = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, 1) }
            } else return null
        }
        val cal = base!!
        // Attach first plausible time; default 10:00.
        TIME.find(text)?.let { m ->
            var h = m.groupValues[1].toIntOrNull() ?: return@let
            val min = m.groupValues[2].toIntOrNull() ?: 0
            when (m.groupValues[3].lowercase()) {
                "pm" -> if (h < 12) h += 12
                "am" -> if (h == 12) h = 0
            }
            if (h in 0..23 && min in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 10); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Suppress("unused")
    fun debugFormats(): String =
        SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).toPattern()
}
