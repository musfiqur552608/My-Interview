package com.freedu.myinterviews.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus

/** Centralised colour mapping so status tags stay consistent across every screen. */
object StatusUi {
    @Composable
    fun applicationContainer(status: ApplicationStatus): Color = when (status) {
        ApplicationStatus.APPLIED -> MaterialTheme.colorScheme.secondaryContainer
        ApplicationStatus.SCREENING -> MaterialTheme.colorScheme.tertiaryContainer
        ApplicationStatus.INTERVIEWING -> MaterialTheme.colorScheme.primaryContainer
        ApplicationStatus.OFFER -> Color(0xFF2E7D32).copy(alpha = 0.18f)
        ApplicationStatus.ACCEPTED -> Color(0xFF2E7D32).copy(alpha = 0.25f)
        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
    }

    @Composable
    fun applicationContent(status: ApplicationStatus): Color = when (status) {
        ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED -> Color(0xFF2E7D32)
        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    fun roundOutcomeColor(outcome: RoundOutcome): Color = when (outcome) {
        RoundOutcome.PASSED -> Color(0xFF2E7D32)
        RoundOutcome.FAILED -> Color(0xFFC62828)
        RoundOutcome.GHOSTED -> Color(0xFF6A6A6A)
        RoundOutcome.PENDING -> Color(0xFF8D6E00)
    }

    fun roundStatusLabel(status: RoundStatus): String = when (status) {
        RoundStatus.UPCOMING -> "Upcoming"
        RoundStatus.COMPLETED -> "Completed"
        RoundStatus.CANCELLED -> "Cancelled"
        RoundStatus.RESCHEDULED -> "Rescheduled"
    }
}
