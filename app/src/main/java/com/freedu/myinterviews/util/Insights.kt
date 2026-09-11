package com.freedu.myinterviews.util

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication

/**
 * Smart scheduling + follow-up detection (pure functions over stored rounds/apps).
 */
object Insights {

    private val ACTIVE = setOf(
        ApplicationStatus.APPLIED, ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEWING
    )

    /** Pairs of upcoming rounds whose time windows overlap. */
    fun findOverlaps(rounds: List<InterviewRound>): List<Pair<InterviewRound, InterviewRound>> {
        val upcoming = rounds.filter { it.status.name == "UPCOMING" }.sortedBy { it.scheduledAt }
        val out = mutableListOf<Pair<InterviewRound, InterviewRound>>()
        for (i in upcoming.indices) {
            val a = upcoming[i]
            val aEnd = a.scheduledAt + a.durationMin.coerceAtLeast(1) * 60_000L
            for (j in i + 1 until upcoming.size) {
                val b = upcoming[j]
                if (b.scheduledAt >= aEnd) break
                out.add(a to b)
            }
        }
        return out
    }

    /**
     * Applications that went quiet: still active, no upcoming round scheduled,
     * and last touch older than [followUpDays].
     */
    fun staleApplications(
        apps: List<JobApplication>,
        rounds: List<InterviewRound>,
        followUpDays: Int,
        now: Long = System.currentTimeMillis()
    ): List<JobApplication> {
        val cutoff = now - followUpDays.coerceAtLeast(1) * 86_400_000L
        val appsWithUpcoming = rounds
            .filter { it.status.name == "UPCOMING" && it.scheduledAt >= now }
            .map { it.applicationId }.toSet()
        return apps.filter { app ->
            app.status in ACTIVE &&
                app.id !in appsWithUpcoming &&
                maxOf(app.appliedDate, app.updatedAt) < cutoff
        }.sortedBy { it.updatedAt }
    }
}
