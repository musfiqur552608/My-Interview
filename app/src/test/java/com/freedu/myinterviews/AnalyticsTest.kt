package com.freedu.myinterviews

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus
import com.freedu.myinterviews.domain.model.RoundType
import com.freedu.myinterviews.util.AnalyticsEngine
import com.freedu.myinterviews.util.AnalyticsRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyticsEngineTest {
    private val now = System.currentTimeMillis()
    private val day = AnalyticsEngine.DAY

    private fun app(
        id: Long,
        status: ApplicationStatus,
        daysAgo: Long,
        company: String = "Acme"
    ) = JobApplication(
        id = id, companyId = 1, companyName = company, jobTitle = "Dev",
        status = status, appliedDate = now - daysAgo * day, updatedAt = now - daysAgo * day
    )

    private fun round(id: Long, appId: Long, daysFromNow: Long) = InterviewRound(
        id = id, applicationId = appId, scheduledAt = now + daysFromNow * day,
        status = if (daysFromNow <= 0) RoundStatus.COMPLETED else RoundStatus.UPCOMING,
        outcome = if (daysFromNow <= 0) RoundOutcome.PASSED else RoundOutcome.PENDING,
        roundType = RoundType.TECHNICAL, selfRating = if (daysFromNow <= 0) 4 else 0
    )

    @Test
    fun rangeFiltersApplications() {
        val apps = listOf(app(1, ApplicationStatus.APPLIED, 5), app(2, ApplicationStatus.APPLIED, 60))
        val d30 = AnalyticsEngine.compute(apps, emptyList(), emptyList(), AnalyticsRange.D30, now)
        assertThat(d30.appsInRange).isEqualTo(1)
        val all = AnalyticsEngine.compute(apps, emptyList(), emptyList(), AnalyticsRange.ALL, now)
        assertThat(all.appsInRange).isEqualTo(2)
    }

    @Test
    fun funnelCountsAndConversion() {
        val apps = listOf(
            app(1, ApplicationStatus.APPLIED, 1),
            app(2, ApplicationStatus.APPLIED, 1),
            app(3, ApplicationStatus.SCREENING, 1),
            app(4, ApplicationStatus.OFFER, 1)
        )
        val d = AnalyticsEngine.compute(apps, emptyList(), emptyList(), AnalyticsRange.ALL, now)
        val byStatus = d.funnel.associate { it.status to it }
        assertThat(byStatus[ApplicationStatus.APPLIED]!!.count).isEqualTo(2)
        assertThat(byStatus[ApplicationStatus.OFFER]!!.count).isEqualTo(1)
        // Screening is 50% of Applied.
        assertThat(byStatus[ApplicationStatus.SCREENING]!!.conversionFromPrev!!)
            .isWithin(0.01f).of(0.5f)
        // Headline conversion = offers / applied cohort.
        assertThat(d.conversion).isWithin(0.01f).of(0.25f)
    }

    @Test
    fun bucketsCoverRangeAndSum() {
        val apps = (1L..5L).map { app(it, ApplicationStatus.APPLIED, it) }
        val d = AnalyticsEngine.compute(apps, emptyList(), emptyList(), AnalyticsRange.D30, now)
        assertThat(d.buckets).hasSize(30)
        assertThat(d.buckets.sumOf { it.count }).isEqualTo(5)
        val all = AnalyticsEngine.compute(apps, emptyList(), emptyList(), AnalyticsRange.ALL, now)
        assertThat(all.buckets).hasSize(12)
    }

    @Test
    fun heatHasTwelveWeeks() {
        val d = AnalyticsEngine.compute(
            listOf(app(1, ApplicationStatus.APPLIED, 0)),
            emptyList(), emptyList(), AnalyticsRange.ALL, now
        )
        assertThat(d.heat).hasSize(84)
        assertThat(d.heat.sumOf { it.count }).isEqualTo(1)
        // Most recent cell is today.
        assertThat(d.heat.last().dayStart).isEqualTo(AnalyticsEngine.startOfDay(now))
    }

    @Test
    fun responseStatsAveragePerCompany() {
        val apps = listOf(
            app(1, ApplicationStatus.INTERVIEWING, 10, "Fast"),
            app(2, ApplicationStatus.INTERVIEWING, 20, "Slow")
        )
        // First rounds 2 and 15 days after applying respectively.
        val rounds = listOf(
            round(1, 1, -8), // scheduled 8 days ago = 2 days after applied
            round(2, 2, -5) // scheduled 5 days ago = 15 days after applied
        )
        val d = AnalyticsEngine.compute(apps, rounds, emptyList(), AnalyticsRange.ALL, now)
        assertThat(d.responses).hasSize(2)
        assertThat(d.responses.first().company).isEqualTo("Fast")
        assertThat(d.responses.first().avgDays).isWithin(0.01).of(2.0)
        assertThat(d.overallResponseDays!!).isWithin(0.01).of(8.5)
    }

    @Test
    fun ratingsAverageByType() {
        val apps = listOf(app(1, ApplicationStatus.INTERVIEWING, 3))
        val rounds = listOf(
            round(1, 1, -2).copy(roundType = RoundType.TECHNICAL, selfRating = 4),
            round(2, 1, -1).copy(roundType = RoundType.TECHNICAL, selfRating = 2),
            round(3, 1, -1).copy(roundType = RoundType.BEHAVIORAL, selfRating = 5)
        )
        val d = AnalyticsEngine.compute(apps, rounds, emptyList(), AnalyticsRange.ALL, now)
        val tech = d.ratings.first { it.type == RoundType.TECHNICAL }
        assertThat(tech.avg).isWithin(0.01f).of(3f)
        assertThat(tech.count).isEqualTo(2)
    }

    @Test
    fun offersSortedByTotalCompDesc() {
        val offers = listOf(
            Offer(id = 1, applicationId = 1, baseSalary = 100_000),
            Offer(id = 2, applicationId = 2, baseSalary = 200_000)
        )
        val d = AnalyticsEngine.compute(emptyList(), emptyList(), offers, AnalyticsRange.ALL, now)
        assertThat(d.offers.map { it.offer.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun emptyStateIsSane() {
        val d = AnalyticsEngine.compute(emptyList(), emptyList(), emptyList(), AnalyticsRange.ALL, now)
        assertThat(d.appsInRange).isEqualTo(0)
        assertThat(d.conversion).isEqualTo(0f)
        assertThat(d.overallResponseDays).isNull()
        assertThat(d.responses).isEmpty()
    }
}
