package com.freedu.myinterviews.presentation.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.AppSettings
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.ThemeMode
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.presentation.components.SectionHeader
import com.freedu.myinterviews.util.CsvBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val repo: TrackerRepository
) : ViewModel() {
    val state = combine(settings.settings, repo.observeCompanies()) { s, _ -> s }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(fn: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(fn) }
    }

    suspend fun snapshot(profileId: String): Pair<List<Company>, List<JobApplication>> =
        combine(repo.observeCompanies(), repo.observeApplications(profileId)) { c, a -> c to a }.first()

    suspend fun snapshotAll(profileId: String) = kotlinx.coroutines.flow.combine(
        repo.observeCompanies(),
        repo.observeApplications(profileId),
        repo.observeAllRounds(),
        repo.observeOffers()
    ) { companies, apps, rounds, offers -> Quad(companies, apps, rounds, offers) }.first()

    data class Quad(
        val companies: List<Company>,
        val apps: List<JobApplication>,
        val rounds: List<com.freedu.myinterviews.domain.model.InterviewRound>,
        val offers: List<com.freedu.myinterviews.domain.model.Offer>
    )

    /** Import: match applications to companies by name, creating missing companies. */
    suspend fun restore(parsed: CsvBackup.ParsedBackup) {
        val existing = repo.observeCompanies().first().associateBy { it.name.lowercase() }.toMutableMap()
        val nameToId = mutableMapOf<String, Long>()
        parsed.companies.forEach { c ->
            val key = c.name.lowercase()
            val id = existing[key]?.id ?: repo.upsertCompany(c)
            existing[key] = c.copy(id = id)
            nameToId[key] = id
        }
        parsed.applications.forEach { a ->
            val key = a.companyName.lowercase()
            val companyId = nameToId[key] ?: existing[key]?.id ?: repo.upsertCompany(
                Company(name = a.companyName.ifBlank { "Unknown" })
            ).also { nameToId[key] = it }
            repo.upsertApplication(a.copy(companyId = companyId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var profileDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                status = runCatching {
                    val (companies, apps) = vm.snapshot(s.profileId)
                    CsvBackup.writeToUri(ctx, uri, CsvBackup.export(companies, apps))
                    "Exported ${apps.size} applications, ${companies.size} companies."
                }.getOrElse { "Export failed: ${it.message}" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                status = runCatching {
                    val text = CsvBackup.readFromUri(ctx, uri)
                    vm.restore(CsvBackup.parse(text))
                    "Import complete."
                }.getOrElse { "Import failed: ${it.message}" }
            }
        }
    }
    val portfolioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                status = runCatching {
                    val snap = vm.snapshotAll(s.profileId)
                    com.freedu.myinterviews.util.PdfReport.writeFullPortfolio(
                        ctx, uri, snap.companies, snap.apps, snap.rounds, snap.offers
                    )
                    "Portfolio PDF exported (${snap.apps.size} applications)."
                }.getOrElse { "Export failed: ${it.message}" }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item { SectionHeader("Appearance") }
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Theme", modifier = Modifier.weight(1f))
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = s.themeMode == mode,
                                onClick = { vm.update { it.copy(themeMode = mode) } },
                                label = { Text(mode.name.lowercase().replaceFirstChar { c -> c.uppercase() }) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Dynamic (Material You) color", modifier = Modifier.weight(1f))
                        Switch(s.dynamicColor, { checked -> vm.update { it.copy(dynamicColor = checked) } })
                    }
                }
            }
            item { SectionHeader("Reminders") }
            item {
                SettingsCard {
                    Text("Remind me ${s.reminderHoursBefore}h before each interview")
                    Slider(
                        value = s.reminderHoursBefore.toFloat(), onValueChange = {},
                        onValueChangeFinished = {},
                        valueRange = 1f..12f, steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Slider is display + tap; use chips for reliable input:
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 4, 12).forEach { h ->
                            FilterChip(selected = s.reminderHoursBefore == h,
                                onClick = { vm.update { it.copy(reminderHoursBefore = h) } },
                                label = { Text("${h}h") })
                        }
                    }
                    ToggleRow("Also remind 1 day before", s.remindDayBefore,
                        { checked -> vm.update { it.copy(remindDayBefore = checked) } })
                    ToggleRow("Thank-you note nudge after rounds", s.thankYouReminder,
                        { checked -> vm.update { it.copy(thankYouReminder = checked) } })
                    ToggleRow("Daily summary notification", s.dailySummary,
                        { checked -> vm.update { it.copy(dailySummary = checked) } })
                    Text("Follow up if no response after ${s.followUpDays} days",
                        style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 7, 14).forEach { d ->
                            FilterChip(selected = s.followUpDays == d,
                                onClick = { vm.update { it.copy(followUpDays = d) } },
                                label = { Text("${d}d") })
                        }
                    }
                }
            }
            item { SectionHeader("Privacy & profiles") }
            item {
                SettingsCard {
                    ToggleRow("Biometric / device app lock", s.appLock,
                        { checked -> vm.update { it.copy(appLock = checked) } })
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Active profile: ${s.profileId}", modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { profileDialog = true }) { Text("Switch") }
                    }
                    Text("Profiles keep separate pipelines (e.g. backend vs android roles).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionHeader("Goals & AI coach") }
            item {
                SettingsCard {
                    Text("Weekly application goal: ${s.weeklyGoal}",
                        style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 7, 10).forEach { g ->
                            FilterChip(selected = s.weeklyGoal == g,
                                onClick = { vm.update { it.copy(weeklyGoal = g) } },
                                label = { Text("$g") })
                        }
                    }
                    var key by remember(s.llmApiKey) { mutableStateOf(s.llmApiKey) }
                    androidx.compose.material3.OutlinedTextField(
                        key, { key = it },
                        label = { Text("AI coach API key (optional, OpenAI-compatible)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { vm.update { it.copy(llmApiKey = key.trim()) } }) {
                                Text("Save")
                            }
                        }
                    )
                    Text("Stored on-device only. Blank = offline coaching questions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionHeader("Backup & export") }
            item {
                SettingsCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TITLE, "interview-tracker-backup.csv")
                            }
                            exportLauncher.launch(intent)
                        }) {
                            androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = null)
                            Text(" Export CSV")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }) {
                            androidx.compose.material3.Icon(Icons.Default.Upload, contentDescription = null)
                            Text(" Import")
                        }
                    }
                    Text("CSV backup includes companies + applications. PDF reports export from any application screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_TITLE, "interview-portfolio.pdf")
                        }
                        portfolioLauncher.launch(intent)
                    }) {
                        Text("Export full portfolio PDF")
                    }
                    if (status != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(status!!, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { SectionHeader("About") }
            item {
                SettingsCard {
                    Text("Interview Tracker v1.0 — offline-first. Your data stays on this device (Room).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
    if (profileDialog) {
        var name by remember { mutableStateOf(s.profileId) }
        AlertDialog(
            onDismissRequest = { profileDialog = false },
            title = { Text("Switch profile") },
            text = {
                androidx.compose.material3.OutlinedTextField(name, { name = it },
                    label = { Text("Profile id (e.g. android, backend)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.update { it.copy(profileId = name.trim().lowercase()) }
                    profileDialog = false
                }) { Text("Switch") }
            },
            dismissButton = { TextButton(onClick = { profileDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange)
    }
}
