package com.freedu.myinterviews.presentation.simport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.util.DateUtils
import com.freedu.myinterviews.util.EmailDraft
import com.freedu.myinterviews.util.EmailParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartImportViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val settings: SettingsDataStore
) : ViewModel() {
    /** Paste → company → application → round, all prefilled. Returns ids for navigation. */
    fun import(draft: EmailDraft, role: String, company: String, onDone: (appId: Long) -> Unit) {
        viewModelScope.launch {
            val profile = runCatching { settings.settings.first().profileId }.getOrDefault("default")
            val existing = repo.observeCompanies().first()
                .firstOrNull { it.name.equals(company, true) }
            val companyId = existing?.id ?: repo.upsertCompany(Company(name = company.ifBlank { "Unknown" }))
            val cname = existing?.name ?: company.ifBlank { "Unknown" }
            val appId = repo.upsertApplication(
                JobApplication(companyId = companyId, companyName = cname, jobTitle = role.ifBlank { "Interview" },
                    jobDescription = "Imported from recruiter email.", profileId = profile)
            )
            draft.scheduledAt?.let { at ->
                repo.upsertRound(
                    InterviewRound(applicationId = appId, companyName = cname,
                        jobTitle = role.ifBlank { "Interview" }, scheduledAt = at,
                        mode = draft.mode, interviewers = draft.interviewer,
                        platformLink = draft.link)
                )
            }
            onDone(appId)
        }
    }
}

/**
 * Smart import: paste a recruiter email → offline parser prefills company, role,
 * date/time, interviewer and video link → one tap creates everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartImportScreen(
    onOpenApplication: (Long) -> Unit,
    vm: SmartImportViewModel = hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf<EmailDraft?>(null) }
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var done by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart import", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("Paste a recruiter or scheduling email below. Links, dates and names are detected on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                text, { text = it; done = null },
                label = { Text("Email text") },
                modifier = Modifier.fillMaxWidth(), minLines = 6
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        val d = EmailParser.parse(text)
                        draft = d
                        role = d.role
                        company = d.company
                    }
                ) {
                    IconOrFallback()
                    Text(" Detect details")
                }
                if (draft != null) {
                    OutlinedButton(
                        onClick = { text = ""; draft = null; role = ""; company = "" },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text("Clear") }
                }
            }
            val d = draft
            if (d != null) {
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Detected (tap to edit before saving)",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(company, { company = it }, label = { Text("Company") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(role, { role = it }, label = { Text("Role") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "When: ${d.scheduledAt?.let { DateUtils.dateTime(it) } ?: "not found"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (d.interviewer.isNotBlank()) Text("With: ${d.interviewer}",
                            style = MaterialTheme.typography.bodyMedium)
                        if (d.link.isNotBlank()) Text("Link: ${d.link.take(60)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Mode: ${d.mode}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch {
                                vm.import(d, role, company) { appId ->
                                    done = "Created — opening application…"
                                    onOpenApplication(appId)
                                }
                            }
                        }) { Text("Create company + application + round") }
                        if (done != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(done!!, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun IconOrFallback() {
    androidx.compose.material3.Icon(Icons.Default.AutoAwesome, contentDescription = null)
}
