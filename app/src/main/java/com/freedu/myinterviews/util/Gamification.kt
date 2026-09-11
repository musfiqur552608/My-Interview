package com.freedu.myinterviews.util

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import java.util.Calendar

/**
 * Streaks, XP and levels computed deterministically from stored data
 * (no extra storage — gamification is a pure view over your history).
 */
object Gamification {

    fun xp(apps: List<JobApplication>, rounds: List<InterviewRound>): Int {
        val completed = rounds.count { it.status.name == "COMPLETED" }
        val passed = rounds.count { it.outcome.name == "PASSED" }
        val reflected = rounds.count { it.questionsAsked.isNotBlank() }
        val offers = apps.count {
            it.status == ApplicationStatus.OFFER || it.status == ApplicationStatus.ACCEPTED
        }
        return apps.size * 10 + completed * 20 + passed * 15 + reflected * 10 + offers * 100
    }

    data class Level(val level: Int, val name: String, val intoLevel: Int, val span: Int = 200) {
        val progress: Float get() = (intoLevel.toFloat() / span).coerceIn(0f, 1f)
    }

    private val NAMES = listOf("Scout", "Applicant", "Contender", "Closer", "Finalist", "Legend")

    fun level(xp: Int): Level {
        val l = (xp / 200) + 1
        return Level(l, NAMES[(l - 1).coerceAtMost(NAMES.size - 1)], xp % 200)
    }

    private fun weekKey(millis: Long): Pair<Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) to c.get(Calendar.WEEK_OF_YEAR)
    }

    private fun weekStart(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Consecutive weeks (up to now) with ≥1 application. */
    fun weekStreak(apps: List<JobApplication>, now: Long = System.currentTimeMillis()): Int {
        if (apps.isEmpty()) return 0
        val weeks = apps.map { weekKey(it.appliedDate) }.toSet()
        var streak = 0
        var cursor = weekStart(now)
        // Allow the current week to be empty only if today is the first days — simpler:
        // streak counts back from the most recent active week, must include recent activity.
        if (weekKey(now) !in weeks) {
            // grace: streak still counts if last activity was last week
            cursor -= 7 * 86_400_000L
            if (weekKey(cursor) !in weeks) return 0
        }
        while (weekKey(cursor) in weeks) {
            streak++
            cursor -= 7 * 86_400_000L
        }
        return streak
    }

    fun weekCount(apps: List<JobApplication>, now: Long = System.currentTimeMillis()): Int {
        val start = weekStart(now)
        return apps.count { it.appliedDate >= start }
    }
}
