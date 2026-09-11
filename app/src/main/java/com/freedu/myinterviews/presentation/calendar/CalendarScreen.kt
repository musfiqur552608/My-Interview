package com.freedu.myinterviews.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenApplication: (Long) -> Unit,
    vm: CalendarViewModel = hiltViewModel()
) {
    val rounds by vm.rounds.collectAsState()
    val offset by vm.monthOffset.collectAsState()
    val selected by vm.selectedDay.collectAsState()
    val selectedRounds by vm.selectedRounds.collectAsState()
    val cells = rememberCells(offset)
    val daysWithRounds = rememberDaysWithRounds(rounds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(monthTitle(offset), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.shiftMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                    }
                }
            )
        }
    ) { pad ->
        val overlaps = rememberOverlaps(rounds)
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            if (overlaps.isNotEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("⚠ ${overlaps.size} scheduling conflict${if (overlaps.size > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold)
                            overlaps.take(3).forEach { (a, b) ->
                                Text(
                                    "• ${a.companyName} (${DateUtils.time(a.scheduledAt)}) overlaps ${b.companyName} (${DateUtils.time(b.scheduledAt)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("Consider rescheduling one round.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                        Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Manual week rows (avoids nesting a scrollable grid inside LazyColumn).
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                if (day == null) {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val isSelected = day == selected
                                    val hasRounds = daysWithRounds.contains(day)
                                    val isToday = day == CalendarViewModel.startOfToday()
                                    Box(
                                        Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .clickable { vm.selectDay(day) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                Calendar.getInstance().apply { timeInMillis = day }
                                                    .get(Calendar.DAY_OF_MONTH).toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hasRounds) {
                                                Box(
                                                    Modifier
                                                        .padding(top = 1.dp)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.primary,
                                                            CircleShape
                                                        )
                                                        .padding(2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // pad short final week
                            repeat(7 - week.size) { Box(Modifier.weight(1f).aspectRatio(1f)) }
                        }
                    }
                }
            }
            item {
                Text(
                    "Schedule for ${DateUtils.date(selected)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            if (selectedRounds.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "Nothing scheduled",
                        subtitle = "Rounds on this day will appear here with quick links."
                    )
                }
            } else {
                items(selectedRounds, key = { it.id }) { r ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onOpenApplication(r.applicationId) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("${r.companyName} · ${r.jobTitle}", fontWeight = FontWeight.SemiBold)
                            Text("${DateUtils.time(r.scheduledAt)} · ${r.roundType} · ${r.mode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (r.platformLink.isNotBlank()) {
                                Text(r.platformLink, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun rememberOverlaps(rounds: List<com.freedu.myinterviews.domain.model.InterviewRound>) =
    androidx.compose.runtime.remember(rounds) {
        com.freedu.myinterviews.util.Insights.findOverlaps(rounds)
    }

@Composable
private fun rememberCells(offset: Int) = androidx.compose.runtime.remember(offset) { monthCells(offset) }

@Composable
private fun rememberDaysWithRounds(rounds: List<com.freedu.myinterviews.domain.model.InterviewRound>) =
    androidx.compose.runtime.remember(rounds) {
        rounds.map {
            val c = Calendar.getInstance().apply { timeInMillis = it.scheduledAt }
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.toSet()
    }
