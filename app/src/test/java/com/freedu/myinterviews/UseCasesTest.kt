package com.freedu.myinterviews

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.model.DashboardStats
import com.freedu.myinterviews.domain.model.DocAttachment
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.PrepTemplate
import com.freedu.myinterviews.domain.model.QuestionBankItem
import com.freedu.myinterviews.domain.repository.SearchResults
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.domain.usecase.LogReflectionUseCase
import com.freedu.myinterviews.domain.usecase.MoveApplicationUseCase
import com.freedu.myinterviews.domain.usecase.ObserveDashboardUseCase
import com.freedu.myinterviews.domain.usecase.ObservePipelineUseCase
import com.freedu.myinterviews.presentation.calendar.monthCells
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Calendar

/** Minimal in-memory fake: only the members under test carry state; the rest are stubs. */
private class FakeRepo(
    apps: List<JobApplication> = emptyList(),
    private val rounds: List<InterviewRound> = emptyList()
) : TrackerRepository {
    private val appsState = apps.toMutableList()
    var lastStatusUpdate: Pair<Long, String>? = null
    var lastRound: InterviewRound? = null

    override fun observeCompanies(): Flow<List<Company>> = flowOf(emptyList())
    override fun observeCompany(id: Long): Flow<Company?> = flowOf(null)
    override suspend fun upsertCompany(company: Company): Long = 1L
    override suspend fun deleteCompany(id: Long) {}
    override fun observeApplications(profileId: String): Flow<List<JobApplication>> = flowOf(appsState)
    override fun observeApplicationsByCompany(companyId: Long): Flow<List<JobApplication>> =
        flowOf(appsState.filter { it.companyId == companyId })
    override fun observeApplication(id: Long): Flow<JobApplication?> =
        flowOf(appsState.firstOrNull { it.id == id })
    override suspend fun upsertApplication(app: JobApplication): Long = app.id
    override suspend fun updateApplicationStatus(id: Long, status: String) {
        lastStatusUpdate = id to status
    }
    override suspend fun deleteApplication(id: Long) {}
    override fun observeRounds(appId: Long): Flow<List<InterviewRound>> =
        flowOf(rounds.filter { it.applicationId == appId })
    override fun observeAllRounds(): Flow<List<InterviewRound>> = flowOf(rounds)
    override fun observeUpcomingRounds(now: Long): Flow<List<InterviewRound>> =
        flowOf(rounds.filter { it.scheduledAt >= now })
    override suspend fun upsertRound(round: InterviewRound): Long {
        lastRound = round
        return round.id
    }
    override suspend fun deleteRound(id: Long) {}
    override fun observeContacts(): Flow<List<Contact>> = flowOf(emptyList())
    override suspend fun upsertContact(contact: Contact): Long = 1L
    override suspend fun deleteContact(contact: Contact) {}
    override fun observeDocuments(appId: Long): Flow<List<DocAttachment>> = flowOf(emptyList())
    override suspend fun upsertDocument(doc: DocAttachment): Long = 1L
    override suspend fun deleteDocument(id: Long) {}
    override fun observeOffers(): Flow<List<Offer>> = flowOf(emptyList())
    override fun observeOffer(appId: Long): Flow<Offer?> = flowOf(null)
    override suspend fun upsertOffer(offer: Offer): Long = 1L
    override fun observeQuestions(): Flow<List<QuestionBankItem>> = flowOf(emptyList())
    override suspend fun upsertQuestion(item: QuestionBankItem): Long = 1L
    override suspend fun deleteQuestion(id: Long) {}
    override fun observeTemplates(): Flow<List<PrepTemplate>> = flowOf(emptyList())
    override suspend fun upsertTemplate(template: PrepTemplate): Long = 1L
    override suspend fun deleteTemplate(id: Long) {}
    override fun search(query: String): Flow<SearchResults> = flowOf(SearchResults())
    override fun observeStats(profileId: String): Flow<DashboardStats> = flowOf(DashboardStats())
    override fun observePractice(appId: Long): Flow<List<com.freedu.myinterviews.domain.model.PracticeSession>> =
        flowOf(emptyList())
    override suspend fun savePractice(session: com.freedu.myinterviews.domain.model.PracticeSession): Long = 1L
    override suspend fun deletePractice(id: Long) {}
}

class UseCasesTest {

    private fun app(id: Long, status: ApplicationStatus) =
        JobApplication(id = id, companyId = 1, companyName = "Acme", jobTitle = "Dev $id", status = status)

    @Test
    fun pipeline_groupsByStatus() = runTest {
        val repo = FakeRepo(listOf(app(1, ApplicationStatus.APPLIED), app(2, ApplicationStatus.OFFER)))
        val grouped = ObservePipelineUseCase(repo)("default").first()
        assertThat(grouped[ApplicationStatus.APPLIED]).hasSize(1)
        assertThat(grouped[ApplicationStatus.OFFER]).hasSize(1)
        assertThat(grouped[ApplicationStatus.REJECTED]).isEmpty()
    }

    @Test
    fun move_delegatesStatusUpdateToRepo() = runTest {
        val repo = FakeRepo()
        MoveApplicationUseCase(repo)(42L, ApplicationStatus.INTERVIEWING)
        assertThat(repo.lastStatusUpdate).isEqualTo(42L to "INTERVIEWING")
    }

    @Test
    fun reflection_marksRoundCompletedAndClampsRating() = runTest {
        val repo = FakeRepo()
        val round = InterviewRound(id = 9, applicationId = 3)
        LogReflectionUseCase(repo)(
            round, questions = "Q?", answers = "A.", rating = 99,
            wentWell = "x", toImprove = "y", thankYouSent = true
        )
        val saved = repo.lastRound!!
        assertThat(saved.status).isEqualTo(com.freedu.myinterviews.domain.model.RoundStatus.COMPLETED)
        assertThat(saved.selfRating).isEqualTo(5)
        assertThat(saved.thankYouSent).isTrue()
    }

    @Test
    fun dashboard_combinesStatsUpcomingAndRecent() = runTest {
        val now = System.currentTimeMillis()
        val repo = FakeRepo(
            apps = listOf(app(1, ApplicationStatus.APPLIED), app(2, ApplicationStatus.APPLIED)),
            rounds = listOf(
                InterviewRound(id = 1, applicationId = 1, scheduledAt = now + 3_600_000),
                InterviewRound(id = 2, applicationId = 2, scheduledAt = now + 7_200_000)
            )
        )
        val dash = ObserveDashboardUseCase(repo)("default").first()
        assertThat(dash.recent).hasSize(2)
        assertThat(dash.upcoming).hasSize(2)
    }
}

class CalendarLogicTest {
    @Test
    fun monthCells_formCompleteWeeksOrdered() {
        val cells = monthCells(0)
        assertThat(cells.size % 7).isEqualTo(0)
        val days = cells.filterNotNull()
        assertThat(days).isNotEmpty()
        // strictly increasing, start-of-day aligned
        days.zipWithNext { a, b -> assertThat(b).isGreaterThan(a) }
        days.forEach { d ->
            val c = Calendar.getInstance().apply { timeInMillis = d }
            assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
            assertThat(c.get(Calendar.MINUTE)).isEqualTo(0)
        }
    }
}
