package com.freedu.myinterviews.domain.usecase

import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.DashboardStats
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Clean-Architecture use cases: thin, unit-testable orchestration over the repository.
 * ViewModels depend on these (not DAOs) so business rules stay out of the UI layer.
 */
class ObservePipelineUseCase @Inject constructor(private val repo: TrackerRepository) {
    operator fun invoke(profileId: String): Flow<Map<ApplicationStatus, List<JobApplication>>> =
        repo.observeApplications(profileId).map { apps ->
            ApplicationStatus.values().associateWith { s -> apps.filter { it.status == s } }
        }
}

class MoveApplicationUseCase @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke(appId: Long, to: ApplicationStatus) {
        repo.updateApplicationStatus(appId, to.name)
    }
}

class ObserveDashboardUseCase @Inject constructor(private val repo: TrackerRepository) {
    data class Dashboard(
        val stats: DashboardStats,
        val upcoming: List<InterviewRound>,
        val recent: List<JobApplication>,
        val all: List<JobApplication> = emptyList()
    )
    operator fun invoke(profileId: String): Flow<Dashboard> =
        combine(
            repo.observeStats(profileId),
            repo.observeUpcomingRounds(),
            repo.observeApplications(profileId)
        ) { stats, upcoming, apps ->
            Dashboard(stats, upcoming.take(5), apps.take(5), apps)
        }
}

class LogReflectionUseCase @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke(
        round: InterviewRound,
        questions: String,
        answers: String,
        rating: Int,
        wentWell: String,
        toImprove: String,
        thankYouSent: Boolean
    ): Long = repo.upsertRound(
        round.copy(
            questionsAsked = questions,
            yourAnswers = answers,
            selfRating = rating.coerceIn(0, 5),
            wentWell = wentWell,
            toImprove = toImprove,
            thankYouSent = thankYouSent,
            status = com.freedu.myinterviews.domain.model.RoundStatus.COMPLETED
        )
    )
}
