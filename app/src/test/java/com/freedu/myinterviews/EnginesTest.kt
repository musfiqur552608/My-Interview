package com.freedu.myinterviews

import com.freedu.myinterviews.ai.AiCoach
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus
import com.freedu.myinterviews.util.CompCalc
import com.freedu.myinterviews.util.EmailParser
import com.freedu.myinterviews.util.Gamification
import com.freedu.myinterviews.util.Insights
import com.freedu.myinterviews.util.MatchScore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchScoreTest {
    @Test
    fun score_rewardsOverlapAndListsMissing() {
        val jd = "We need Kotlin, Compose and Kafka experience for this Android role."
        val resume = "Android developer with Kotlin and Compose, 5 years."
        val r = MatchScore.score(resume, jd)
        assertThat(r.score).isGreaterThan(0)
        assertThat(r.matched).contains("kotlin")
        assertThat(r.missing).contains("kafka")
    }

    @Test
    fun blankResume_scoresZeroWithAllMissing() {
        val r = MatchScore.score("", "Kotlin Compose Kafka")
        assertThat(r.score).isEqualTo(0)
        assertThat(r.missing).isNotEmpty()
    }

    @Test
    fun blankJd_scoresZero() {
        assertThat(MatchScore.score("Kotlin", "").score).isEqualTo(0)
    }
}

class GamificationTest {
    private fun app(id: Long, dayOffset: Long) = JobApplication(
        id = id, companyId = 1, companyName = "A", jobTitle = "Dev",
        appliedDate = System.currentTimeMillis() + dayOffset * 86_400_000L
    )

    @Test
    fun xp_countsActions() {
        val rounds = listOf(
            InterviewRound(id = 1, applicationId = 1,
                status = RoundStatus.COMPLETED, outcome = RoundOutcome.PASSED,
                questionsAsked = "Q?")
        )
        val xp = Gamification.xp(listOf(app(1, 0)), rounds)
        // 10 (app) + 20 (completed) + 15 (passed) + 10 (reflection)
        assertThat(xp).isEqualTo(55)
    }

    @Test
    fun level_thresholds() {
        assertThat(Gamification.level(0).level).isEqualTo(1)
        assertThat(Gamification.level(199).level).isEqualTo(1)
        assertThat(Gamification.level(200).level).isEqualTo(2)
        assertThat(Gamification.level(0).progress).isEqualTo(0f)
    }

    @Test
    fun streak_countsRecentWeeks() {
        assertThat(Gamification.weekStreak(emptyList())).isEqualTo(0)
        assertThat(Gamification.weekStreak(listOf(app(1, 0)))).isEqualTo(1)
        assertThat(Gamification.weekCount(listOf(app(1, 0), app(2, -30)))).isEqualTo(1)
    }
}

class CompCalcTest {
    @Test
    fun parseMoney_handlesFormats() {
        assertThat(CompCalc.parseMoney("\$150k")).isEqualTo(150_000)
        assertThat(CompCalc.parseMoney("150,000")).isEqualTo(150_000)
        assertThat(CompCalc.parseMoney("1.5M")).isEqualTo(1_500_000)
        assertThat(CompCalc.parseMoney("competitive")).isEqualTo(0)
    }

    @Test
    fun annualized_spreadsEquityOverFourYears() {
        assertThat(CompCalc.annualized(150_000, 20_000, "200000")).isEqualTo(220_000)
    }

    @Test
    fun marketHint_matchesRole() {
        val hint = CompCalc.marketHint("Senior Android Engineer")
        assertThat(hint).isNotNull()
        assertThat(hint!!.second).isGreaterThan(100_000L)
        assertThat(CompCalc.marketHint("Astronaut")).isNull()
    }

    @Test
    fun formatShort_abbreviates() {
        assertThat(CompCalc.formatShort(150_000)).isEqualTo("\$150k")
        assertThat(CompCalc.formatShort(0)).isEqualTo("—")
    }
}

class InsightsTest {
    private fun round(id: Long, at: Long, mins: Int = 60) = InterviewRound(
        id = id, applicationId = id, scheduledAt = at, durationMin = mins,
        status = RoundStatus.UPCOMING
    )

    @Test
    fun overlaps_detectedOnlyWhenWindowsIntersect() {
        val now = System.currentTimeMillis()
        val a = round(1, now + 3_600_000)
        val b = round(2, now + 3_600_000 + 30 * 60_000) // 30 min into a
        val c = round(3, now + 5 * 3_600_000)
        val overlaps = Insights.findOverlaps(listOf(a, b, c))
        assertThat(overlaps).hasSize(1)
        assertThat(overlaps.first().first.id).isEqualTo(1)
        assertThat(overlaps.first().second.id).isEqualTo(2)
    }

    @Test
    fun stale_excludesActiveMotion() {
        val now = System.currentTimeMillis()
        val old = 10 * 86_400_000L
        val quiet = JobApplication(id = 1, companyId = 1, companyName = "Q", jobTitle = "D",
            status = ApplicationStatus.APPLIED,
            appliedDate = now - old, updatedAt = now - old)
        val withUpcoming = quiet.copy(id = 2)
        val fresh = quiet.copy(id = 3, updatedAt = now)
        val rounds = listOf(
            round(9, now + 3_600_000).copy(applicationId = 2)
        )
        val stale = Insights.staleApplications(
            listOf(quiet, withUpcoming, fresh), rounds, followUpDays = 7, now = now
        )
        assertThat(stale.map { it.id }).containsExactly(1L)
    }
}

class EmailParserTest {
    private val sample = """
        Hi Alex,
        I'm Sarah Chen, a recruiter at NovaTech. We'd love to speak with you
        for the Senior Android Engineer position.
        Are you available this Friday at 3:30pm for a video call with John Miller?
        Here's the Zoom link: https://zoom.us/j/123456789
        Best, Sarah
    """.trimIndent()

    @Test
    fun parsesAllFields() {
        val d = EmailParser.parse(sample)
        assertThat(d.company).contains("NovaTech")
        assertThat(d.role.lowercase()).contains("android")
        assertThat(d.link).contains("zoom.us")
        assertThat(d.interviewer).isNotEmpty()
        assertThat(d.scheduledAt).isNotNull()
    }

    @Test
    fun blank_returnsEmpty() {
        assertThat(EmailParser.parse("").company).isEmpty()
    }
}

class AiCoachTest {
    @Test
    fun generatesRoleSpecificQuestions() {
        val qs = AiCoach.generateQuestions("Android Engineer", "Kotlin Compose CI")
        assertThat(qs).isNotEmpty()
        assertThat(qs.size).isAtMost(8)
        assertThat(qs.joinToString(" ").lowercase()).contains("kotlin")
    }

    @Test
    fun score_averagesRatings() {
        assertThat(AiCoach.scorePractice(listOf(5, 5, 5, 5))).isEqualTo(100)
        assertThat(AiCoach.scorePractice(listOf(3, 3))).isEqualTo(60)
        assertThat(AiCoach.scorePractice(emptyList())).isEqualTo(0)
    }
}
