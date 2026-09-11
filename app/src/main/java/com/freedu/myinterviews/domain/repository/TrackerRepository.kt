package com.freedu.myinterviews.domain.repository

import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.model.DashboardStats
import com.freedu.myinterviews.domain.model.DocAttachment
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.PrepTemplate
import com.freedu.myinterviews.domain.model.QuestionBankItem
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    // Companies
    fun observeCompanies(): Flow<List<Company>>
    fun observeCompany(id: Long): Flow<Company?>
    suspend fun upsertCompany(company: Company): Long
    suspend fun deleteCompany(id: Long)

    // Applications
    fun observeApplications(profileId: String): Flow<List<JobApplication>>
    fun observeApplicationsByCompany(companyId: Long): Flow<List<JobApplication>>
    fun observeApplication(id: Long): Flow<JobApplication?>
    suspend fun upsertApplication(app: JobApplication): Long
    suspend fun updateApplicationStatus(id: Long, status: String)
    suspend fun deleteApplication(id: Long)

    // Rounds
    fun observeRounds(appId: Long): Flow<List<InterviewRound>>
    fun observeAllRounds(): Flow<List<InterviewRound>>
    fun observeUpcomingRounds(now: Long = System.currentTimeMillis()): Flow<List<InterviewRound>>
    suspend fun upsertRound(round: InterviewRound): Long
    suspend fun deleteRound(id: Long)

    // Contacts / docs / offers
    fun observeContacts(): Flow<List<Contact>>
    suspend fun upsertContact(contact: Contact): Long
    suspend fun deleteContact(contact: Contact)
    fun observeDocuments(appId: Long): Flow<List<DocAttachment>>
    suspend fun upsertDocument(doc: DocAttachment): Long
    suspend fun deleteDocument(id: Long)
    fun observeOffers(): Flow<List<Offer>>
    fun observeOffer(appId: Long): Flow<Offer?>
    suspend fun upsertOffer(offer: Offer): Long

    // Prep hub
    fun observeQuestions(): Flow<List<QuestionBankItem>>
    suspend fun upsertQuestion(item: QuestionBankItem): Long
    suspend fun deleteQuestion(id: Long)
    fun observeTemplates(): Flow<List<PrepTemplate>>
    suspend fun upsertTemplate(template: PrepTemplate): Long
    suspend fun deleteTemplate(id: Long)

    // Search + stats
    fun search(query: String): Flow<SearchResults>
    fun observeStats(profileId: String): Flow<DashboardStats>

    // AI coach practice sessions
    fun observePractice(appId: Long): Flow<List<com.freedu.myinterviews.domain.model.PracticeSession>>
    suspend fun savePractice(session: com.freedu.myinterviews.domain.model.PracticeSession): Long
    suspend fun deletePractice(id: Long)
}

data class SearchResults(
    val companies: List<Company> = emptyList(),
    val applications: List<JobApplication> = emptyList(),
    val rounds: List<InterviewRound> = emptyList(),
    val contacts: List<Contact> = emptyList()
)
