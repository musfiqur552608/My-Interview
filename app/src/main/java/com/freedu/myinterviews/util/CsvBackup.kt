package com.freedu.myinterviews.util

import android.content.Context
import android.net.Uri
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.JobApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CSV backup/restore via Storage Access Framework (no storage permission needed).
 * Format v1: two sections — COMPANIES then APPLICATIONS — each with a header row.
 */
object CsvBackup {
    private const val SEP = ","
    private fun esc(v: String): String {
        val s = v.replace("\"", "\"\"")
        return "\"$s\""
    }
    private fun esc(v: Long): String = v.toString()

    fun export(companies: List<Company>, apps: List<JobApplication>): String {
        val sb = StringBuilder()
        sb.appendLine("[COMPANIES]")
        sb.appendLine("id${SEP}name${SEP}website${SEP}industry${SEP}notes${SEP}source${SEP}tags${SEP}createdAt")
        companies.forEach { c ->
            sb.appendLine(
                listOf(
                    esc(c.id), esc(c.name), esc(c.website), esc(c.industry),
                    esc(c.notes), esc(c.source), esc(c.tags.joinToString("|")), esc(c.createdAt)
                ).joinToString(SEP)
            )
        }
        sb.appendLine("[APPLICATIONS]")
        sb.appendLine("id${SEP}companyId${SEP}companyName${SEP}jobTitle${SEP}appliedDate${SEP}status${SEP}salaryMin${SEP}salaryMax${SEP}location${SEP}workMode${SEP}profileId")
        apps.forEach { a ->
            sb.appendLine(
                listOf(
                    esc(a.id), esc(a.companyId), esc(a.companyName), esc(a.jobTitle),
                    esc(a.appliedDate), esc(a.status.name), esc(a.salaryMin), esc(a.salaryMax),
                    esc(a.location), esc(a.workMode.name), esc(a.profileId)
                ).joinToString(SEP)
            )
        }
        return sb.toString()
    }

    /** Minimal CSV row parser handling quoted fields. Pure + unit-tested. */
    fun parseRow(row: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val ch = row[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < row.length && row[i + 1] == '"') {
                        cur.append('"'); i++
                    } else inQuotes = !inQuotes
                }
                ch == ',' && !inQuotes -> { out.add(cur.toString()); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }

    data class ParsedBackup(
        val companies: List<Company>,
        val applications: List<JobApplication>
    )

    fun parse(text: String): ParsedBackup {
        val companies = mutableListOf<Company>()
        val apps = mutableListOf<JobApplication>()
        var section = ""
        text.lines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("[")) { section = line; return@forEach }
            if (line.startsWith("id,")) return@forEach // header
            val cols = parseRow(line)
            runCatching {
                if (section == "[COMPANIES]" && cols.size >= 8) {
                    companies.add(
                        Company(
                            id = 0, // fresh ids on import to avoid collisions
                            name = cols[1], website = cols[2], industry = cols[3],
                            notes = cols[4], source = cols[5],
                            tags = cols[6].split("|").filter { it.isNotBlank() },
                            createdAt = cols[7].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    )
                } else if (section == "[APPLICATIONS]" && cols.size >= 11) {
                    apps.add(
                        JobApplication(
                            id = 0, companyId = -1, companyName = cols[2],
                            jobTitle = cols[3],
                            appliedDate = cols[4].toLongOrNull() ?: System.currentTimeMillis(),
                            status = runCatching {
                                com.freedu.myinterviews.domain.model.ApplicationStatus.valueOf(cols[5])
                            }.getOrDefault(com.freedu.myinterviews.domain.model.ApplicationStatus.APPLIED),
                            salaryMin = cols[6].toLongOrNull() ?: 0,
                            salaryMax = cols[7].toLongOrNull() ?: 0,
                            location = cols[8],
                            workMode = runCatching {
                                com.freedu.myinterviews.domain.model.WorkMode.valueOf(cols[9])
                            }.getOrDefault(com.freedu.myinterviews.domain.model.WorkMode.HYBRID),
                            profileId = cols[10].ifBlank { "default" }
                        )
                    )
                }
            }
        }
        return ParsedBackup(companies, apps)
    }

    suspend fun writeToUri(ctx: Context, uri: Uri, content: String) = withContext(Dispatchers.IO) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            ?: error("Cannot open file for writing")
    }

    suspend fun readFromUri(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Cannot open file for reading")
    }
}
