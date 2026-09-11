package com.freedu.myinterviews.presentation.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.ai.AiCoach
import com.freedu.myinterviews.domain.model.PracticeSession
import com.freedu.myinterviews.presentation.company.CompanyViewModel
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.util.DateUtils
import kotlinx.coroutines.launch

/**
 * AI interview coach: offline-generated mock questions tailored to the role +
 * JD, self-rated answers, session score history, and optional LLM feedback
 * when an API key is set in Settings (graceful offline fallback otherwise).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    onBack: () -> Unit,
    vm: CompanyViewModel = hiltViewModel()
) {
    val app by vm.application.collectAsState()
    val sessions by vm.practice.collectAsState()
    val settings by vm.settingsFlow.collectAsState()
    var started by remember { mutableStateOf(false) }
    var questions by remember { mutableStateOf(emptyList<String>()) }
    val answers = remember { mutableStateMapOf<Int, String>() }
    val ratings = remember { mutableStateMapOf<Int, Int>() }
    var result by remember { mutableStateOf<Int?>(null) }
    var aiFeedback by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock interview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        val a = app
        if (a == null) {
            Column(Modifier.padding(pad).padding(24.dp)) { Text("Application not found.") }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("${a.jobTitle} @ ${a.companyName}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp))
            }
            if (!started) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Answer out loud (or in writing), rate yourself honestly, get a score. Questions are generated on-device from your role and JD.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            questions = AiCoach.generateQuestions(a.jobTitle, a.jobDescription)
                            answers.clear(); ratings.clear(); result = null; aiFeedback = null
                            started = true
                        }, modifier = Modifier.fillMaxWidth()) { Text("Start mock interview") }
                    }
                }
            } else {
                itemsIndexed(questions, key = { i, _ -> i }) { i, q ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Q${i + 1}. $q", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                answers[i].orEmpty(), { answers[i] = it },
                                label = { Text("Your answer (bullet points ok)") },
                                modifier = Modifier.fillMaxWidth(), minLines = 2
                            )
                            Row {
                                Text("Rating: ${ratings[i] ?: "–"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 12.dp, end = 4.dp))
                                (1..5).forEach { s ->
                                    TextButton(onClick = { ratings[i] = s }) {
                                        Text(if (s <= (ratings[i] ?: 0)) "★" else "☆")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(16.dp)) {
                        Button(
                            enabled = ratings.isNotEmpty(),
                            onClick = {
                                val score = AiCoach.scorePractice(questions.indices.map { ratings[it] ?: 0 })
                                result = score
                                vm.savePractice(
                                    PracticeSession(
                                        applicationId = a.id, role = a.jobTitle,
                                        questions = questions,
                                        answers = questions.indices.map { answers[it].orEmpty() },
                                        score = score
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Finish & score") }
                        result?.let { score ->
                            Spacer(Modifier.height(8.dp))
                            Text("Session score: $score/100",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val key = settings?.llmApiKey.orEmpty()
                                    if (key.isBlank()) {
                                        aiFeedback = "Tip: record one specific metric per answer (latency cut, users served, crash-free %). " +
                                            "Add an API key in Settings → AI coach for model-written feedback."
                                    } else {
                                        aiLoading = true
                                        scope.launch {
                                            aiFeedback = AiCoach.coachReply(
                                                key, a.jobTitle,
                                                questions.mapIndexed { idx, q -> q to answers[idx].orEmpty() }
                                            ) ?: "Model call failed (offline or bad key). Your session score above is still saved."
                                            aiLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Get AI feedback") }
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            }
                            aiFeedback?.let {
                                Spacer(Modifier.height(8.dp))
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Text(it, Modifier.padding(14.dp))
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { started = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("New session") }
                    }
                }
            }
            item {
                Text("Past sessions (${sessions.size})",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp))
            }
            if (sessions.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.Psychology, title = "No practice yet",
                        subtitle = "Your scored sessions will appear here so you can track progress.")
                }
            } else {
                itemsIndexed(sessions, key = { _, s -> s.id }) { _, s ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Score ${s.score}/100", fontWeight = FontWeight.Bold)
                                Text("${s.questions.size} questions · ${DateUtils.date(s.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
