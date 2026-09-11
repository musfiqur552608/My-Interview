package com.freedu.myinterviews.data.repository

import com.freedu.myinterviews.data.local.TrackerDatabase
import com.freedu.myinterviews.data.local.toDomain
import com.freedu.myinterviews.data.local.toEntity
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
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundType
import com.freedu.myinterviews.domain.repository.SearchResults
import com.freedu.myinterviews.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepositoryImpl @Inject constructor(
    private val db: TrackerDatabase
) : TrackerRepository {

    // ---- Companies ----
    override fun observeCompanies() =
        db.companyDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCompany(id: Long) =
        db.companyDao().observeById(id).map { it?.toDomain() }

    override suspend fun upsertCompany(company: Company): Long =
        db.companyDao().upsert(company.toEntity())

    override suspend fun deleteCompany(id: Long) {
        // Manual cascade (keeps FK-free schema simple & robust for v1).
        db.applicationDao().deleteByCompany(id)
        db.companyDao().deleteById(id)
        // Note: rounds/documents/offers whose parent application was removed are
        // filtered out of joined flows (mapNotNull), so no orphan rows surface in UI.
    }

    // ---- Applications ----
    override fun observeApplications(profileId: String): Flow<List<JobApplication>> =
        combine(
            db.applicationDao().observeAll(profileId),
            db.companyDao().observeAll()
        ) { apps, companies ->
            val names = companies.associate { it.id to it.name }
            apps.map { it.toDomain(names[it.companyId].orEmpty()) }
        }

    override fun observeApplicationsByCompany(companyId: Long): Flow<List<JobApplication>> =
        combine(
            db.applicationDao().observeByCompany(companyId),
            db.companyDao().observeById(companyId)
        ) { apps, company ->
            apps.map { it.toDomain(company?.name.orEmpty()) }
        }

    override fun observeApplication(id: Long): Flow<JobApplication?> =
        combine(
            db.applicationDao().observeById(id),
            db.companyDao().observeAll()
        ) { app, companies ->
            app?.toDomain(companies.firstOrNull { it.id == app.companyId }?.name.orEmpty())
        }

    override suspend fun upsertApplication(app: JobApplication): Long =
        db.applicationDao().upsert(app.copy(updatedAt = System.currentTimeMillis()).toEntity())

    override suspend fun updateApplicationStatus(id: Long, status: String) {
        db.applicationDao().updateStatus(id, status)
    }

    override suspend fun deleteApplication(id: Long) {
        db.roundDao().deleteByApplication(id)
        db.documentDao().observeByApplication(id) // no-op read to keep ordering explicit
        db.offerDao().deleteByApplication(id)
        db.applicationDao().deleteById(id)
    }

    // ---- Rounds ----
    override fun observeRounds(appId: Long): Flow<List<InterviewRound>> =
        combine(
            db.roundDao().observeByApplication(appId),
            db.applicationDao().observeEvery(),
            db.companyDao().observeAll()
        ) { rounds, apps, companies ->
            // Note: join across profiles intentionally broad; filtered by appId already.
            val app = apps.firstOrNull { it.id == appId }
            val companyName = companies.firstOrNull { it.id == app?.companyId }?.name.orEmpty()
            rounds.map { it.toDomain(companyName, app?.jobTitle.orEmpty()) }
        }

    override fun observeAllRounds(): Flow<List<InterviewRound>> =
        combine(
            db.roundDao().observeAll(),
            db.applicationDao().observeEvery(),
            db.companyDao().observeAll()
        ) { rounds, apps, companies ->
            val appById = apps.associateBy { it.id }
            val companyById = companies.associateBy { it.id }
            rounds.mapNotNull { r ->
                val app = appById[r.applicationId] ?: return@mapNotNull null
                r.toDomain(companyById[app.companyId]?.name.orEmpty(), app.jobTitle)
            }
        }

    override fun observeUpcomingRounds(now: Long): Flow<List<InterviewRound>> =
        observeAllRounds().map { list ->
            list.filter { it.scheduledAt >= now - 60_000 && it.status.name == "UPCOMING" }
                .sortedBy { it.scheduledAt }
        }

    override suspend fun upsertRound(round: InterviewRound): Long =
        db.roundDao().upsert(round.toEntity())

    override suspend fun deleteRound(id: Long) = db.roundDao().deleteById(id)

    // ---- Contacts / docs / offers ----
    override fun observeContacts(): Flow<List<Contact>> =
        combine(db.contactDao().observeAll(), db.companyDao().observeAll()) { contacts, companies ->
            val names = companies.associate { it.id to it.name }
            contacts.map { it.toDomain(it.companyId?.let { id -> names[id] }.orEmpty()) }
        }

    override suspend fun upsertContact(contact: Contact): Long =
        db.contactDao().upsert(contact.toEntity())

    override suspend fun deleteContact(contact: Contact) {
        db.contactDao().delete(contact.toEntity())
    }

    override fun observeDocuments(appId: Long) =
        db.documentDao().observeByApplication(appId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertDocument(doc: DocAttachment): Long =
        db.documentDao().upsert(doc.toEntity())

    override suspend fun deleteDocument(id: Long) = db.documentDao().deleteById(id)

    override fun observeOffers(): Flow<List<Offer>> =
        combine(
            db.offerDao().observeAll(),
            db.applicationDao().observeEvery(),
            db.companyDao().observeAll()
        ) { offers, apps, companies ->
            val appById = apps.associateBy { it.id }
            val companyById = companies.associateBy { it.id }
            offers.mapNotNull { o ->
                val app = appById[o.applicationId] ?: return@mapNotNull null
                o.toDomain(companyById[app.companyId]?.name.orEmpty(), app.jobTitle)
            }
        }

    override fun observeOffer(appId: Long) =
        combine(
            db.offerDao().observeByApplication(appId),
            db.applicationDao().observeById(appId)
        ) { offer, app -> offer?.toDomain(jobTitle = app?.jobTitle.orEmpty()) }

    override suspend fun upsertOffer(offer: Offer): Long = db.offerDao().upsert(offer.toEntity())

    // ---- Prep hub ----
    override fun observeQuestions() =
        db.questionDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertQuestion(item: QuestionBankItem): Long =
        db.questionDao().upsert(item.toEntity())

    override suspend fun deleteQuestion(id: Long) = db.questionDao().deleteById(id)

    override fun observeTemplates() =
        db.prepTemplateDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertTemplate(template: PrepTemplate): Long =
        db.prepTemplateDao().upsert(template.toEntity())

    override suspend fun deleteTemplate(id: Long) = db.prepTemplateDao().deleteById(id)

    // ---- AI coach practice ----
    override fun observePractice(appId: Long) =
        db.practiceDao().observeByApplication(appId).map { list -> list.map { it.toDomain() } }

    override suspend fun savePractice(session: com.freedu.myinterviews.domain.model.PracticeSession): Long =
        db.practiceDao().upsert(session.toEntity())

    override suspend fun deletePractice(id: Long) = db.practiceDao().deleteById(id)

    // ---- Search + stats ----
    override fun search(query: String): Flow<SearchResults> {
        if (query.isBlank()) {
            return combine(observeCompanies(), observeApplications("default"), observeAllRounds(), observeContacts()) { c, a, r, ct ->
                SearchResults(c.take(5), a.take(5), r.take(5), ct.take(5))
            }
        }
        return combine(
            db.companyDao().search(query),
            db.applicationDao().searchApplications(query),
            db.roundDao().search(query),
            db.contactDao().search(query)
        ) { companies, apps, rounds, contacts ->
            SearchResults(
                companies.map { it.toDomain() },
                apps.map { it.toDomain() },
                rounds.map { it.toDomain() },
                contacts.map { it.toDomain() }
            )
        }
    }

    override fun observeStats(profileId: String): Flow<DashboardStats> =
        combine(
            observeApplications(profileId),
            observeAllRounds()
        ) { apps, rounds ->
            val statusCounts = ApplicationStatus.values().associateWith { s -> apps.count { it.status == s } }
            val total = apps.size
            val offers = (statusCounts[ApplicationStatus.OFFER] ?: 0) + (statusCounts[ApplicationStatus.ACCEPTED] ?: 0)
            val active = (statusCounts[ApplicationStatus.APPLIED] ?: 0) +
                (statusCounts[ApplicationStatus.SCREENING] ?: 0) +
                (statusCounts[ApplicationStatus.INTERVIEWING] ?: 0)
            val now = System.currentTimeMillis()
            val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
            val passRate = RoundType.values().associateWith { type ->
                val typed = rounds.filter { it.roundType == type && it.status.name == "COMPLETED" }
                if (typed.isEmpty()) 0f
                else typed.count { it.outcome == RoundOutcome.PASSED }.toFloat() / typed.size
            }
            val upcoming = rounds.count { it.status.name == "UPCOMING" && it.scheduledAt >= now }
            DashboardStats(
                totalApplications = total,
                activeCount = active,
                interviewsUpcoming = upcoming,
                offersCount = offers,
                offerRate = if (total == 0) 0f else offers.toFloat() / total,
                statusCounts = statusCounts,
                passRateByRoundType = passRate,
                applicationsThisWeek = apps.count { it.appliedDate >= weekAgo },
                mostCommonRejectionStage = statusCounts.maxByOrNull { it.value }?.key?.name ?: "-"
            )
        }
}
