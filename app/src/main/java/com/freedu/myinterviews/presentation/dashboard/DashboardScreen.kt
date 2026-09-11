package com.freedu.myinterviews.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.presentation.components.CompanyAvatar
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.FunnelChart
import com.freedu.myinterviews.presentation.components.LoadingState
import com.freedu.myinterviews.presentation.components.PassRateBars
import com.freedu.myinterviews.presentation.components.SectionHeader
import com.freedu.myinterviews.presentation.components.StatCard
import com.freedu.myinterviews.presentation.components.StatusChip
import com.freedu.myinterviews.util.DateUtils
import com.freedu.myinterviews.util.Gamification
import com.freedu.myinterviews.util.Insights

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenApplication: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenPrep: () -> Unit = {},
    onOpenOffers: () -> Unit = {},
    onOpenToday: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    vm: DashboardViewModel = hiltViewModel()
) {
    val data by vm.state.collectAsState()
    val allRounds by vm.allRounds.collectAsState()
    val settings by vm.settingsFlow.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onQuickAdd) {
                Icon(Icons.Default.Add, contentDescription = "Quick add")
            }
        }
    ) { pad ->
        val d = data
        if (d == null) {
            LoadingState(Modifier.padding(pad))
            return@Scaffold
        }
        if (d.stats.totalApplications == 0) {
            Column(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(
                    icon = Icons.Default.Business,
                    title = "Track your first application",
                    subtitle = "Add a company and role to start building your pipeline, reminders and prep notes."
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("${d.stats.totalApplications}", "Applied", Modifier.weight(1f))
                    StatCard("${d.stats.interviewsUpcoming}", "Upcoming", Modifier.weight(1f))
                    StatCard("${(d.stats.offerRate * 100).toInt()}%", "Offer rate", Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("${d.stats.activeCount}", "Active", Modifier.weight(1f))
                    StatCard("${d.stats.applicationsThisWeek}", "This week 🔥", Modifier.weight(1f))
                    StatCard("${d.stats.offersCount}", "Offers", Modifier.weight(1f))
                }
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ActionChip(Icons.Default.School, "Prep Hub", onOpenPrep)
                    }
                    item {
                        ActionChip(Icons.Default.CompareArrows, "Offers", onOpenOffers)
                    }
                    item {
                        ActionChip(Icons.Default.Today, "Day mode", onOpenToday)
                    }
                    item {
                        ActionChip(Icons.Default.AutoAwesome, "Smart import", onOpenImport)
                    }
                    item {
                        ActionChip(Icons.Default.Analytics, "Analytics", onOpenAnalytics)
                    }
                }
            }
            item {
                val xp = Gamification.xp(d.all, allRounds)
                val level = Gamification.level(xp)
                val streak = Gamification.weekStreak(d.all)
                val weekDone = Gamification.weekCount(d.all)
                val goal = settings?.weeklyGoal ?: 5
                GamificationCard(level, xp, streak, weekDone, goal)
            }
            run {
                val stale = Insights.staleApplications(
                    d.all, allRounds, settings?.followUpDays ?: 7
                )
                if (stale.isNotEmpty()) {
                    item {
                        FollowUpCard(stale.take(3), stale.size, onOpenApplication)
                    }
                }
            }
            item {
                SectionHeader("Pipeline funnel")
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) { FunnelChart(d.stats.statusCounts) }
            }
            item {
                SectionHeader("Success by round")
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) { PassRateBars(d.stats.passRateByRoundType) }
            }
            item { SectionHeader("Next interviews") }
            if (d.upcoming.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "No upcoming interviews",
                        subtitle = "Schedule a round from any application to get reminders here."
                    )
                }
            } else {
                items(d.upcoming, key = { it.id }) { r ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onOpenApplication(r.applicationId) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            CompanyAvatar(r.companyName.ifBlank { "?" })
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text("${r.companyName} · ${r.jobTitle}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${r.roundType.name.lowercase().replace('_', ' ')} · ${DateUtils.dateTime(r.scheduledAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Recently updated") }
            items(d.recent, key = { it.id }) { app ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenApplication(app.id) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        CompanyAvatar(app.companyName.ifBlank { app.jobTitle })
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(app.jobTitle, fontWeight = FontWeight.SemiBold)
                            Text(app.companyName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusChip(app.status)
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GamificationCard(
    level: Gamification.Level,
    xp: Int,
    streak: Int,
    weekDone: Int,
    goal: Int
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Level ${level.level} · ${level.name}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("$xp XP", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { level.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "🔥 $streak-week streak · $weekDone/$goal applications this week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FollowUpCard(
    stale: List<com.freedu.myinterviews.domain.model.JobApplication>,
    total: Int,
    onOpenApplication: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        ),
        onClick = { if (stale.isNotEmpty()) onOpenApplication(stale.first().id) }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (total == 1) "1 application needs a follow-up"
                    else "$total applications need a follow-up",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            stale.forEach { app ->
                Text("• ${app.companyName} — ${app.jobTitle} (quiet since ${DateUtils.date(app.updatedAt)})",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
