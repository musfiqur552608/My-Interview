package com.freedu.myinterviews.presentation.company

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.model.DocAttachment
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val settings: SettingsDataStore,
    @ApplicationContext private val ctx: Context,
    savedState: SavedStateHandle
) : ViewModel() {
    val companyId: Long = savedState.get<Long>("companyId")
        ?: savedState.get<String>("companyId")?.toLongOrNull() ?: 0L
    val appId: Long = savedState.get<Long>("appId")
        ?: savedState.get<String>("appId")?.toLongOrNull() ?: 0L

    val company: StateFlow<Company?> =
        if (companyId > 0) repo.observeCompany(companyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        else MutableStateFlow<Company?>(null).asStateFlow()

    val companyApps: StateFlow<List<JobApplication>> =
        if (companyId > 0) repo.observeApplicationsByCompany(companyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        else MutableStateFlow(emptyList<JobApplication>()).asStateFlow()

    val application: StateFlow<JobApplication?> =
        if (appId > 0) repo.observeApplication(appId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        else MutableStateFlow<JobApplication?>(null).asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rounds: StateFlow<List<InterviewRound>> =
        if (appId > 0) repo.observeRounds(appId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        else MutableStateFlow(emptyList<InterviewRound>()).asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<DocAttachment>> =
        if (appId > 0) repo.observeDocuments(appId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        else MutableStateFlow(emptyList<DocAttachment>()).asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val offer: StateFlow<Offer?> =
        if (appId > 0) repo.observeOffer(appId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        else MutableStateFlow<Offer?>(null).asStateFlow()

    val practice: StateFlow<List<com.freedu.myinterviews.domain.model.PracticeSession>> =
        if (appId > 0) repo.observePractice(appId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        else MutableStateFlow(emptyList<com.freedu.myinterviews.domain.model.PracticeSession>()).asStateFlow()

    fun savePractice(session: com.freedu.myinterviews.domain.model.PracticeSession) {
        viewModelScope.launch { repo.savePractice(session) }
    }

    val contacts: StateFlow<List<Contact>> = repo.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settingsFlow = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- CRUD (all through repository → Room; reminders scheduled on round save) ----
    fun saveCompany(c: Company, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.upsertCompany(if (c.id == 0L) c else c)
            onDone(if (c.id == 0L) id else c.id)
        }
    }

    fun deleteCompany(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.deleteCompany(id); onDone() }
    }

    fun saveApplication(app: JobApplication, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.upsertApplication(app)
            onDone(if (app.id == 0L) id else app.id)
        }
    }

    fun deleteApplication(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            ReminderScheduler.cancelForRound(ctx, id)
            repo.deleteApplication(id); onDone()
        }
    }

    fun saveRound(round: InterviewRound) {
        viewModelScope.launch {
            val id = repo.upsertRound(round)
            val savedId = if (round.id == 0L) id else round.id
            val s = runCatching {
                settings.settings.first()
            }.getOrNull()
            // Schedule: X hours before + optionally 1 day before; thank-you nudge 2h after.
            val title = "Interview: ${round.jobTitle.ifBlank { "upcoming round" }}"
            val detail = "${round.companyName} · ${round.roundType} · ${round.mode}"
            if (round.status.name == "UPCOMING") {
                val hours = (s?.reminderHoursBefore ?: 1).toLong()
                ReminderScheduler.schedule(
                    ctx, savedId, round.scheduledAt - hours * 3_600_000L, title, detail
                )
                if (s?.remindDayBefore != false) {
                    ReminderScheduler.schedule(
                        ctx, savedId, round.scheduledAt - 24 * 3_600_000L,
                        "Tomorrow: $title", detail
                    )
                }
                if (s?.thankYouReminder != false) {
                    ReminderScheduler.schedule(
                        ctx, savedId, round.scheduledAt + 2 * 3_600_000L,
                        "Send your thank-you note", "Follow up on: $title"
                    )
                }
            } else {
                ReminderScheduler.cancelForRound(ctx, savedId)
            }
            // Keep the home-screen widget + persisted summary in sync (best-effort).
            runCatching {
                val upcoming = repo.observeUpcomingRounds().first()
                val next = upcoming.firstOrNull()
                val summary = next?.let {
                    "${it.companyName} · ${it.jobTitle} · " +
                        com.freedu.myinterviews.util.DateUtils.dateTime(it.scheduledAt)
                } ?: "No upcoming interviews"
                settings.update { it.copy(nextInterviewSummary = summary) }
                com.freedu.myinterviews.widget.NextInterviewWidget.refreshWidget(
                    ctx, summary, next?.scheduledAt ?: 0
                )
            }
        }
    }

    fun deleteRound(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancelForRound(ctx, id)
            repo.deleteRound(id)
        }
    }

    fun saveOffer(o: Offer) {
        viewModelScope.launch { repo.upsertOffer(o) }
    }

    fun saveContact(c: Contact) {
        viewModelScope.launch { repo.upsertContact(c) }
    }

    fun saveDocument(d: DocAttachment) {
        viewModelScope.launch { repo.upsertDocument(d) }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch { repo.deleteDocument(id) }
    }
}
