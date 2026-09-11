package com.freedu.myinterviews.presentation.today

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(repo: TrackerRepository) : ViewModel() {
    val rounds = repo.observeAllRounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Interview-day mode: everything for today's rounds on one screen —
 * times, interviewers, join links, checklist progress and prep notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenApplication: (Long) -> Unit,
    vm: TodayViewModel = hiltViewModel()
) {
    val rounds by vm.rounds.collectAsState()
    val ctx = LocalContext.current
    val start = Calendar.getInstance().let {
        it.set(Calendar.HOUR_OF_DAY, 0); it.set(Calendar.MINUTE, 0)
        it.set(Calendar.SECOND, 0); it.set(Calendar.MILLISECOND, 0)
        it.timeInMillis
    }
    val today = rounds.filter {
        it.scheduledAt in start until start + 86_400_000L && it.status.name != "CANCELLED"
    }.sortedBy { it.scheduledAt }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Interview day", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        if (today.isEmpty()) {
            Column(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(
                    icon = Icons.Default.Today,
                    title = "No interviews today",
                    subtitle = "Enjoy the breather — prep for what's next from any application."
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(today, key = { it.id }) { r ->
                val checks = r.prepChecklistJson.split(";;").filter { it.isNotBlank() }
                val done = checks.count { it.startsWith("✓") }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    onClick = { onOpenApplication(r.applicationId) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "${DateUtils.time(r.scheduledAt)} · ${r.roundType.name.lowercase().replace('_', ' ')}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${r.companyName} — ${r.jobTitle}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (r.interviewers.isNotBlank()) {
                            Text("With ${r.interviewers}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (checks.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { done.toFloat() / checks.size },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Checklist $done/${checks.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (r.prepNotes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(r.prepNotes.take(220), style = MaterialTheme.typography.bodySmall)
                        }
                        if (r.platformLink.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, r.platformLink.toUri()))
                                }
                            }) {
                                Icon(Icons.Default.VideoCall, contentDescription = null)
                                Text(" Join ${r.mode}")
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Good luck — you've prepared for this. Reflect right after each round.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}
