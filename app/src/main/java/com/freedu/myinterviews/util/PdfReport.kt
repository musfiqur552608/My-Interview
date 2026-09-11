package com.freedu.myinterviews.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Styled PDF reports (Storage Access Framework target Uri, fully offline).
 *
 * House style: dark title band (#16123A, the app mark) with cream title and an
 * amber rule, shaded section headers, card-outlined round entries, page
 * footers. Shared [Doc] helper keeps the single-application report and the
 * full portfolio visually consistent.
 */
object PdfReport {

    private const val INK = 0xFF1B1B1F
    private const val MUTED = 0xFF55555C
    private const val BAND = 0xFF16123A
    private const val CREAM = 0xFFEDEAE3
    private const val AMBER = 0xFFFFB020
    private const val WASH = 0xFFF1EEF7

    private class Doc(private val ctx: Context, private val uri: Uri) {
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var pageNum = 1
        var y = 56f

        private val titlePaint = Paint().apply {
            textSize = 22f; color = CREAM.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val subtitlePaint = Paint().apply { textSize = 11f; color = 0xFFCFC8E8.toInt() }
        private val headPaint = Paint().apply {
            textSize = 14f; color = BAND.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val bodyPaint = Paint().apply { textSize = 11f; color = INK.toInt() }
        private val mutedPaint = Paint().apply { textSize = 10f; color = MUTED.toInt() }
        private val footPaint = Paint().apply { textSize = 9f; color = MUTED.toInt() }
        private val bandPaint = Paint().apply { color = BAND.toInt() }
        private val amberPaint = Paint().apply { color = AMBER.toInt(); strokeWidth = 3f }
        private val washPaint = Paint().apply { color = WASH.toInt() }
        private val rulePaint = Paint().apply { color = 0xFFD8D3E8.toInt(); strokeWidth = 1f }
        private val cardPaint = Paint().apply {
            color = 0xFF8E8E96.toInt(); strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        fun finishPage() {
            page.canvas.drawText(
                "Page $pageNum · Interview Tracker", 40f, 822f, footPaint
            )
            doc.finishPage(page)
        }

        fun newPage() {
            finishPage()
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            y = 56f
        }

        private fun need(h: Float) {
            if (y + h > 790) newPage()
        }

        private fun wrapped(text: String, paint: Paint, indent: Float, gap: Float = 6f) {
            var remaining = text
            while (remaining.isNotEmpty()) {
                need(paint.textSize + gap)
                // Greedy wrap on word boundaries (~72 chars).
                var cut = minOf(72, remaining.length)
                if (remaining.length > 72) {
                    val space = remaining.take(72).lastIndexOf(' ')
                    if (space > 40) cut = space
                }
                page.canvas.drawText(remaining.take(cut).trimEnd(), indent, y, paint)
                y += paint.textSize + gap
                remaining = remaining.drop(cut).trimStart()
            }
        }

        fun titleBand(title: String, subtitle: String) {
            page.canvas.drawRect(0f, 0f, 595f, 150f, bandPaint)
            page.canvas.drawText(title.take(38), 40f, 72f, titlePaint)
            page.canvas.drawText(subtitle.take(70), 40f, 96f, subtitlePaint)
            page.canvas.drawLine(40f, 116f, 180f, 116f, amberPaint)
            y = 178f
        }

        fun section(title: String) {
            need(46f)
            y += 8f
            page.canvas.drawRect(40f, y - 18f, 555f, y + 8f, washPaint)
            page.canvas.drawText(title.take(60), 52f, y, headPaint)
            y += 8f
            page.canvas.drawLine(40f, y, 555f, y, rulePaint)
            y += 12f
        }

        fun body(text: String, indent: Float = 40f) = wrapped(text, bodyPaint, indent)
        fun muted(text: String, indent: Float = 40f) = wrapped(text, mutedPaint, indent)

        fun kv(k: String, v: String, indent: Float = 52f) {
            need(20f)
            val bold = Paint(bodyPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            page.canvas.drawText(k.take(24), indent, y, bold)
            page.canvas.drawText(v.take(52), indent + 150f, y, bodyPaint)
            y += 17f
        }

        fun card(lines: List<String>) {
            val h = lines.size * 17f + 20f
            need(h)
            page.canvas.drawRoundRect(40f, y, 555f, y + h, 10f, 10f, cardPaint)
            y += 20f
            lines.forEach { line ->
                page.canvas.drawText(line.take(72), 56f, y, bodyPaint)
                y += 17f
            }
            y += 14f
        }

        fun close() {
            finishPage()
            ctx.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                ?: error("Cannot open file for writing")
            doc.close()
        }
    }

    suspend fun writeApplicationReport(
        ctx: Context,
        uri: Uri,
        app: JobApplication,
        rounds: List<InterviewRound>
    ) = withContext(Dispatchers.IO) {
        val d = Doc(ctx, uri)
        d.titleBand(
            "${app.jobTitle}".take(38),
            "${app.companyName} · ${app.status} · Applied ${DateUtils.date(app.appliedDate)}"
        )
        d.section("Role overview")
        d.kv("Company", app.companyName)
        d.kv("Location", "${app.location.ifBlank { "—" }} (${app.workMode})")
        if (app.salaryMax > 0) d.kv("Salary", "${app.salaryMin} – ${app.salaryMax}")
        if (app.jobLink.isNotBlank()) d.kv("Posting", app.jobLink.take(52))
        if (app.resumeVersion.isNotBlank()) d.kv("Resume", app.resumeVersion)
        if (app.jobDescription.isNotBlank()) {
            d.section("Description")
            d.muted(app.jobDescription.take(900))
        }
        d.section("Interview rounds (${rounds.size})")
        if (rounds.isEmpty()) {
            d.muted("No rounds scheduled yet.")
        }
        rounds.forEachIndexed { i, r ->
            d.card(
                listOfNotNull(
                    "${i + 1}. ${r.roundType} — ${DateUtils.dateTime(r.scheduledAt)}",
                    "Status ${r.status}/${r.outcome}" +
                        (if (r.selfRating > 0) " · self-rating ${r.selfRating}/5" else "") +
                        (if (r.thankYouSent) " · thanked ✓" else ""),
                    r.interviewers.takeIf { it.isNotBlank() }?.let { "With: $it" },
                    r.questionsAsked.takeIf { it.isNotBlank() }?.let { "Q: ${it.take(140)}" },
                    r.wentWell.takeIf { it.isNotBlank() }?.let { "+ ${it.take(140)}" },
                    r.toImprove.takeIf { it.isNotBlank() }?.let { "Δ ${it.take(140)}" }
                )
            )
        }
        d.close()
    }

    /**
     * Full job-search portfolio: stats summary + every application with its
     * rounds and offers — for career coaches or personal records.
     */
    suspend fun writeFullPortfolio(
        ctx: Context,
        uri: Uri,
        companies: List<Company>,
        apps: List<JobApplication>,
        rounds: List<InterviewRound>,
        offers: List<Offer>
    ) = withContext(Dispatchers.IO) {
        val d = Doc(ctx, uri)
        val offerCount = offers.size
        d.titleBand(
            "Job Search Portfolio",
            "Generated ${DateUtils.date(System.currentTimeMillis())} · " +
                "${apps.size} applications · ${companies.size} companies · $offerCount offers"
        )
        d.section("At a glance")
        val byStatus = apps.groupBy { it.status }.toList().sortedByDescending { it.second.size }
        byStatus.forEach { (s, list) -> d.kv(s.name.lowercase(), "${list.size}") }
        val rate = if (apps.isEmpty()) 0 else offerCount * 100 / apps.size
        d.kv("Offer rate", "$rate%")
        d.kv("This week", "${apps.count { it.appliedDate >= System.currentTimeMillis() - 7 * 86_400_000L }}")

        val roundsByApp = rounds.groupBy { it.applicationId }
        val offerByApp = offers.associateBy { it.applicationId }
        apps.sortedByDescending { it.updatedAt }.forEach { app ->
            d.section("${app.jobTitle} @ ${app.companyName}")
            d.muted("Status ${app.status} · Applied ${DateUtils.date(app.appliedDate)} · " +
                "${app.location.ifBlank { "—" }} (${app.workMode})")
            roundsByApp[app.id].orEmpty().sortedBy { it.scheduledAt }.forEach { r ->
                d.body("• ${r.roundType} ${DateUtils.date(r.scheduledAt)} — ${r.status}/${r.outcome}" +
                    (if (r.selfRating > 0) " (${r.selfRating}/5)" else ""), indent = 52f)
            }
            offerByApp[app.id]?.let { o ->
                d.body("★ Offer: base ${o.baseSalary}, bonus ${o.bonus}, " +
                    "equity ${o.equity.ifBlank { "—" }} · ${DateUtils.countdown(o.deadline)}",
                    indent = 52f)
            }
        }
        d.close()
    }
}
