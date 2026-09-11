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
 * Full-history PDF report for one application (Storage Access Framework target Uri).
 * Uses framework PdfDocument — no extra dependency, works fully offline.
 */
object PdfReport {
    suspend fun writeApplicationReport(
        ctx: Context,
        uri: Uri,
        app: JobApplication,
        rounds: List<InterviewRound>
    ) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = doc.startPage(pageInfo)
        var y = 56f
        val title = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val head = Paint().apply { textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint().apply { textSize = 11f }

        fun draw(text: String, paint: Paint, indent: Float = 40f) {
            if (y > 800) {
                doc.finishPage(page)
                page = doc.startPage(pageInfo)
                y = 56f
            }
            // crude word-wrap at ~75 chars
            var remaining = text
            while (remaining.isNotEmpty()) {
                val chunk = if (remaining.length > 75) remaining.take(75) else remaining
                remaining = if (remaining.length > 75) remaining.drop(75) else ""
                page.canvas.drawText(chunk, indent, y, paint)
                y += paint.textSize + 6
            }
            y += 4
        }

        draw("Interview Report — ${app.jobTitle}", title)
        draw("${app.companyName} · ${app.status} · Applied ${DateUtils.date(app.appliedDate)}", body)
        if (app.location.isNotBlank()) draw("Location: ${app.location} (${app.workMode})", body)
        if (app.salaryMax > 0) draw("Salary: ${app.salaryMin} – ${app.salaryMax}", body)
        if (app.jobLink.isNotBlank()) draw("Link: ${app.jobLink}", body)
        y += 8
        draw("Interview rounds (${rounds.size})", head)
        rounds.forEachIndexed { i, r ->
            draw("${i + 1}. ${r.roundType} — ${DateUtils.dateTime(r.scheduledAt)} [${r.status}/${r.outcome}]", body)
            if (r.interviewers.isNotBlank()) draw("   Interviewers: ${r.interviewers}", body)
            if (r.questionsAsked.isNotBlank()) draw("   Q: ${r.questionsAsked.take(300)}", body)
            if (r.wentWell.isNotBlank()) draw("   Went well: ${r.wentWell.take(300)}", body)
            if (r.toImprove.isNotBlank()) draw("   Improve: ${r.toImprove.take(300)}", body)
            if (r.selfRating > 0) draw("   Self-rating: ${r.selfRating}/5", body)
        }
        doc.finishPage(page)
        ctx.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            ?: error("Cannot open file for writing")
        doc.close()
    }

    /**
     * Full job-search portfolio: every company, application, round outcome and
     * offer in one document — for career coaches or personal records.
     */
    suspend fun writeFullPortfolio(
        ctx: Context,
        uri: Uri,
        companies: List<Company>,
        apps: List<JobApplication>,
        rounds: List<InterviewRound>,
        offers: List<Offer>
    ) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = doc.startPage(pageInfo)
        var y = 56f
        val title = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val head = Paint().apply { textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint().apply { textSize = 11f }

        fun draw(text: String, paint: Paint, indent: Float = 40f) {
            if (y > 800) {
                doc.finishPage(page)
                page = doc.startPage(pageInfo)
                y = 56f
            }
            var remaining = text
            while (remaining.isNotEmpty()) {
                val chunk = if (remaining.length > 75) remaining.take(75) else remaining
                remaining = if (remaining.length > 75) remaining.drop(75) else ""
                page.canvas.drawText(chunk, indent, y, paint)
                y += paint.textSize + 6
            }
            y += 4
        }

        val roundsByApp = rounds.groupBy { it.applicationId }
        val offerByApp = offers.associateBy { it.applicationId }
        draw("Job Search Portfolio", title)
        draw("Generated ${DateUtils.date(System.currentTimeMillis())} · ${apps.size} applications · ${companies.size} companies · ${offers.size} offers", body)
        y += 8
        apps.forEach { app ->
            draw("${app.jobTitle} @ ${app.companyName} [${app.status}]", head)
            draw("Applied ${DateUtils.date(app.appliedDate)} · ${app.location.ifBlank { "—" }} · ${app.workMode}", body)
            roundsByApp[app.id].orEmpty().forEach { r ->
                draw("  - ${r.roundType} ${DateUtils.date(r.scheduledAt)}: ${r.status}/${r.outcome}" +
                    (if (r.selfRating > 0) " (${r.selfRating}/5)" else ""), body)
            }
            offerByApp[app.id]?.let { o ->
                draw("  Offer: base ${o.baseSalary}, bonus ${o.bonus}, equity ${o.equity.ifBlank { "—" }}", body)
            }
            y += 4
        }
        doc.finishPage(page)
        ctx.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            ?: error("Cannot open file for writing")
        doc.close()
    }
}
