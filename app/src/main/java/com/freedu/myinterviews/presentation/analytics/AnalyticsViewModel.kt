package com.freedu.myinterviews.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.util.AnalyticsEngine
import com.freedu.myinterviews.util.AnalyticsRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repo: TrackerRepository,
    settings: SettingsDataStore
) : ViewModel() {
    val range = MutableStateFlow(AnalyticsRange.D90)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data = combine(
        settings.settings.flatMapLatest { repo.observeApplications(it.profileId) },
        repo.observeAllRounds(),
        repo.observeOffers(),
        range
    ) { apps, rounds, offers, r ->
        AnalyticsEngine.compute(apps, rounds, offers, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(r: AnalyticsRange) {
        range.value = r
    }
}
