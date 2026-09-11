package com.freedu.myinterviews.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.domain.usecase.ObserveDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    settings: SettingsDataStore,
    repo: TrackerRepository
) : ViewModel() {
    val state = settings.settings.flatMapLatest { s ->
        observeDashboard(s.profileId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** All rounds (not just upcoming) — powers follow-up detection + gamification. */
    val allRounds = repo.observeAllRounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settingsFlow = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
