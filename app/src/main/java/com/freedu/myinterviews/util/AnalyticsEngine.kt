package com.freedu.myinterviews.util

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.RoundType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Time window for analytics. Null days = all time. */
enum class AnalyticsRange(val days: Long?, val label: String) {
    D30(30, "30D"),
    D90(90, "90D"),
    M6(180, "6M"),
    ALL(null, "All")
}

data class TimeBucket(
    val start: Long,
    val end: Long,
    val label: String,
    val count: Int
)

data class FunnelStage(
    val status: ApplicationStatus,
    val count: Int,
    /** Share of the previous funnel stage (null for the first). */
    val conversionFromPrev: Float?
)

data class OfferBar(
    val offer: Offer,
    val totalAnnual: Long
)

data class HeatCell(
    val dayStart: Long,
    val count: Int
)

data class ResponseStat(
    val company: String,
    val avgDays: Double,
    val samples: Int
)

data class RatingStat(
    val type: RoundType,
    val avg: Float,
    val count: Int
)

data class AnalyticsData(
    val range: AnalyticsRange,
    val appsInRange: Int,
    val interviewsInRange: Int,
    val offersTotal: Int,
    val conversion: Float, // offers / applied (headline)
    val buckets: List<TimeBucket>,
    val funnel: List<FunnelStage>,
    val offers: List<OfferBar>,
    val heat: List<HeatCell>, // 84 cells, oldest → newest
    val responses: List<ResponseStat>,
    val overallResponseDays: Double?,
    val ratings: List<RatingStat>,
    val bestDay: String
)

/**
 * Pure analytics engine: everything the dashboard computes, range-aware and
 * fully unit-testable (no Android framework beyond java.util/java.text).
 */
object AnalyticsEngine {

    const val DAY = 86_400_000L
    private const val HEAT_DAYS = 84 // 12 weeks

    fun compute(
        apps: List<JobApplication>,
        rounds: List<InterviewRound>,
        offers: List<Offer>,
        range: AnalyticsRange,
        now: Long = System.currentTimeMillis()
    ): AnalyticsData {
        val cutoff = range.days?.let { now - it * DAY }
        val inRange = if (cutoff == null) apps else apps.filter { it.appliedDate >= cutoff }
        val roundsInRange = rounds.filter {
            val lo = cutoff ?: Long.MIN_VALUE
            it.scheduledAt >= lo && it.scheduledAt <= now + 366 * DAY
        }

        val applied = inRange.size
        val offerCount = inRange.count {
            it.status == ApplicationStatus.OFFER || it.status == ApplicationStatus.ACCEPTED
        }

        return AnalyticsData(
            range = range,
            appsInRange = applied,
            interviewsInRange = roundsInRange.size,
            offersTotal = offerCount,
            conversion = if (applied == 0) 0f else offerCount.toFloat() / applied,
            buckets = buckets(inRange, range, now),
            funnel = funnel(inRange),
            offers = offers.sortedByDescending {
                CompCalc.annualized(it.baseSalary, it.bonus, it.equity)
            }.map { OfferBar(it, CompCalc.annualized(it.baseSalary, it.bonus, it.equity)) },
            heat = heat(apps, now),
            responses = responses(apps, rounds),
            overallResponseDays = overallResponse(apps, rounds),
            ratings = ratings(rounds),
            bestDay = bestDay(inRange)
        )
    }

    // ---- Applications over time ----

    private fun buckets(
        apps: List<JobApplication>,
        range: AnalyticsRange,
        now: Long
    ): List<TimeBucket> = when (range) {
        AnalyticsRange.D30 -> (29 downTo 0).map { i ->
            val start = startOfDay(now - i * DAY)
            TimeBucket(start, start + DAY, fmt("d MMM", start),
                apps.count { it.appliedDate in start until start + DAY })
        }
        AnalyticsRange.D90 -> windows(apps, now, 13, 7, "d MMM")
        AnalyticsRange.M6 -> windows(apps, now, 26, 7, "d MMM")
        AnalyticsRange.ALL -> months(apps, now)
    }

    private fun windows(
        apps: List<JobApplication>,
        now: Long,
        n: Int,
        spanDays: Int,
        pattern: String
    ): List<TimeBucket> {
        val today = startOfDay(now)
        return (n - 1 downTo 0).map { i ->
            val end = today - i * spanDays * DAY + DAY
            val start = end - spanDays * DAY
            TimeBucket(start, end, fmt(pattern, start),
                apps.count { it.appliedDate in start until end })
        }
    }

    private fun months(apps: List<JobApplication>, now: Long): List<TimeBucket> {
        return (11 downTo 0).map { i ->
            val c = Calendar.getInstance().apply { timeInMillis = now }
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.add(Calendar.MONTH, -i)
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            val start = c.timeInMillis
            c.add(Calendar.MONTH, 1)
            val end = c.timeInMillis
            TimeBucket(start, end, fmt("MMM ''yy", start),
                apps.count { it.appliedDate in start until end })
        }
    }

    // ---- Funnel ----

    private fun funnel(apps: List<JobApplication>): List<FunnelStage> {
        val stages = ApplicationStatus.funnelOrder
        var prev = -1
        return stages.map { s ->
            val count = apps.count { it.status == s }
            val conv = if (prev < 0) null
            else if (prev == 0) 0f else count.toFloat() / prev
            prev = count
            FunnelStage(s, count, conv)
        }
    }

    // ---- Activity heatmap (last 12 weeks) ----

    private fun heat(apps: List<JobApplication>, now: Long): List<HeatCell> {
        val today = startOfDay(now)
        return (HEAT_DAYS - 1 downTo 0).map { i ->
            val start = today - i * DAY
            HeatCell(start, apps.count { it.appliedDate in start until start + DAY })
        }
    }

    // ---- Response times: applied → first round, per company ----

    private fun firstRoundByApp(rounds: List<InterviewRound>): Map<Long, Long> =
        rounds.groupBy { it.applicationId }
            .mapValues { (_, rs) -> rs.minOf { it.scheduledAt } }

    private fun responses(
        apps: List<JobApplication>,
        rounds: List<InterviewRound>
    ): List<ResponseStat> {
        val first = firstRoundByApp(rounds)
        return apps.mapNotNull { app ->
            val f = first[app.id] ?: return@mapNotNull null
            val days = ((f - app.appliedDate).toDouble() / DAY).coerceAtLeast(0.0)
            app.companyName.ifBlank { "Unknown" } to days
        }.groupBy({ it.first }, { it.second })
            .map { (company, ds) -> ResponseStat(company, ds.average(), ds.size) }
            .sortedBy { it.avgDays }
            .take(5)
    }

    private fun overallResponse(
        apps: List<JobApplication>,
        rounds: List<InterviewRound>
    ): Double? {
        val first = firstRoundByApp(rounds)
        val ds = apps.mapNotNull { app ->
            first[app.id]?.let { ((it - app.appliedDate).toDouble() / DAY).coerceAtLeast(0.0) }
        }
        return if (ds.isEmpty()) null else ds.average()
    }

    // ---- Self-ratings by round type ----

    private fun ratings(rounds: List<InterviewRound>): List<RatingStat> =
        rounds.filter { it.status.name == "COMPLETED" && it.selfRating > 0 }
            .groupBy { it.roundType }
            .map { (t, rs) -> RatingStat(t, rs.map { it.selfRating }.average().toFloat(), rs.size) }
            .sortedByDescending { it.avg }

    private fun bestDay(apps: List<JobApplication>): String {
        if (apps.isEmpty()) return "—"
        val names = arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return apps.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.appliedDate }
                .get(Calendar.DAY_OF_WEEK)
        }.maxByOrNull { it.value.size }?.let { names[it.key] } ?: "—"
    }

    // ---- Helpers ----

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun fmt(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(java.util.Date(millis))
}
