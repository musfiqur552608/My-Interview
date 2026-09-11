package com.freedu.myinterviews.presentation.pipeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.WorkMode
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.domain.usecase.MoveApplicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortBy { RECENT, OLDEST, SALARY, COMPANY }
enum class ViewMode { LIST, KANBAN }

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val mover: MoveApplicationUseCase,
    private val settings: SettingsDataStore,
    private val savedState: SavedStateHandle
) : ViewModel() {

    val query = MutableStateFlow(savedState["q"] ?: "")
    val statusFilter = MutableStateFlow<ApplicationStatus?>(null)
    val workModeFilter = MutableStateFlow<WorkMode?>(null)
    val sortBy = MutableStateFlow(SortBy.RECENT)
    val viewMode = MutableStateFlow(ViewMode.KANBAN)

    fun setQuery(q: String) {
        savedState["q"] = q // survives process death
        query.value = q
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val applications: StateFlow<List<JobApplication>> =
        combine(
            settings.settings.flatMapLatest { repo.observeApplications(it.profileId) },
            query, statusFilter, workModeFilter, sortBy
        ) { apps, q, status, mode, sort ->
            var list = apps
            if (status != null) list = list.filter { it.status == status }
            if (mode != null) list = list.filter { it.workMode == mode }
            if (q.isNotBlank()) {
                list = list.filter {
                    it.jobTitle.contains(q, true) || it.companyName.contains(q, true) ||
                        it.location.contains(q, true)
                }
            }
            when (sort) {
                SortBy.RECENT -> list.sortedByDescending { it.updatedAt }
                SortBy.OLDEST -> list.sortedBy { it.appliedDate }
                SortBy.SALARY -> list.sortedByDescending { it.salaryMax }
                SortBy.COMPANY -> list.sortedBy { it.companyName }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    fun move(app: JobApplication, to: ApplicationStatus) {
        viewModelScope.launch {
            mover(app.id, to)
            _notice.value = "Moved to ${to.name.lowercase()}"
        }
    }

    fun clearNotice() { _notice.value = null }
}
