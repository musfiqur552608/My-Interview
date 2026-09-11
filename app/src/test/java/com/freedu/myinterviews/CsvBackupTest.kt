package com.freedu.myinterviews

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus
import com.freedu.myinterviews.domain.model.RoundType
import com.freedu.myinterviews.domain.model.WorkMode
import com.freedu.myinterviews.util.CsvBackup
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvBackupTest {

    @Test
    fun parseRow_handlesQuotedCommasAndEscapedQuotes() {
        val row = "\"a, b\",\"c\"\"d\",plain"
        assertThat(CsvBackup.parseRow(row)).containsExactly("a, b", "c\"d", "plain")
    }

    @Test
    fun export_parse_roundTripPreservesCoreFields() {
        val companies = listOf(
            Company(name = "Acme, Inc.", website = "https://acme.test", industry = "SaaS",
                notes = "Great \"culture\"", source = "Referral", tags = listOf("remote", "kotlin"))
        )
        val apps = listOf(
            JobApplication(companyId = 7, companyName = "Acme, Inc.", jobTitle = "Android Engineer",
                status = ApplicationStatus.INTERVIEWING, salaryMin = 100000, salaryMax = 140000,
                location = "Berlin", workMode = WorkMode.REMOTE, profileId = "android")
        )
        val parsed = CsvBackup.parse(applicationsCsv(companies, apps))
        assertThat(parsed.companies).hasSize(1)
        assertThat(parsed.companies.first().name).isEqualTo("Acme, Inc.")
        assertThat(parsed.companies.first().tags).containsExactly("remote", "kotlin")
        assertThat(parsed.applications).hasSize(1)
        assertThat(parsed.applications.first().jobTitle).isEqualTo("Android Engineer")
        assertThat(parsed.applications.first().status).isEqualTo(ApplicationStatus.INTERVIEWING)
        assertThat(parsed.applications.first().workMode).isEqualTo(WorkMode.REMOTE)
    }

    @Test
    fun parse_emptyText_returnsEmpty() {
        val parsed = CsvBackup.parse("")
        assertThat(parsed.companies).isEmpty()
        assertThat(parsed.applications).isEmpty()
    }

    private fun applicationsCsv(c: List<Company>, a: List<JobApplication>) =
        CsvBackup.export(c, a)
}

class StatusModelTest {
    @Test
    fun fromName_fallsBackToSensibleDefaults() {
        assertThat(ApplicationStatus.fromName("NOPE")).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(RoundType.fromName(null)).isEqualTo(RoundType.TECHNICAL)
        assertThat(RoundStatus.fromName("")).isEqualTo(RoundStatus.UPCOMING)
        assertThat(RoundOutcome.fromName("???")).isEqualTo(RoundOutcome.PENDING)
        assertThat(WorkMode.fromName("REMOTE")).isEqualTo(WorkMode.REMOTE)
    }

    @Test
    fun funnelOrder_containsCoreStagesInOrder() {
        val funnel = ApplicationStatus.funnelOrder
        assertThat(funnel).containsAtLeast(
            ApplicationStatus.APPLIED, ApplicationStatus.SCREENING,
            ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER
        ).inOrder()
    }
}
