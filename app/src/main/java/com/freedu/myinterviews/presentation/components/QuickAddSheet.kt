package com.freedu.myinterviews.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.presentation.company.ApplicationFormDialog
import com.freedu.myinterviews.presentation.company.CompanyFormDialog
import com.freedu.myinterviews.presentation.company.ContactFormDialog
import com.freedu.myinterviews.presentation.company.RoundFormDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val settings: SettingsDataStore
) : ViewModel() {
    val companies = repo.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCompany(c: Company, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.upsertCompany(c); onDone() }
    }
    fun addApplication(app: com.freedu.myinterviews.domain.model.JobApplication, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.upsertApplication(app); onDone() }
    }
    fun addRound(round: com.freedu.myinterviews.domain.model.InterviewRound, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.upsertRound(round); onDone() }
    }
    fun addContact(contact: com.freedu.myinterviews.domain.model.Contact, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.upsertContact(contact); onDone() }
    }
    suspend fun profileId(): String = runCatching { settings.settings.first().profileId }.getOrDefault("default")
}

/**
 * Global quick-add bottom sheet available from every tab via FAB.
 * Step 1: pick what to create. Step 2: the matching form dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onOpenApplication: (Long) -> Unit = {},
    vm: QuickAddViewModel = hiltViewModel()
) {
    val companies by vm.companies.collectAsState()
    var step by remember { mutableStateOf(0) } // 0=choose, 1=company, 2=application, 3=round, 4=contact
    var companyPick by remember { mutableStateOf<Company?>(null) }
    var appPick by remember { mutableStateOf<com.freedu.myinterviews.domain.model.JobApplication?>(null) }
    var pickOpen by remember { mutableStateOf(false) }
    var pickMode by remember { mutableStateOf("app") } // app|round
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    if (step == 0) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Quick add", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Company" to 1, "Application" to 2, "Interview round" to 3, "Contact" to 4
                ).forEach { (label, to) ->
                    OutlinedButton(
                        onClick = {
                            if ((to == 2 || to == 3) && companies.isEmpty()) {
                                step = 1 // need a company first
                            } else step = to
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        return
    }

    when (step) {
        1 -> CompanyFormDialog(onDismiss = onDismiss) { vm.addCompany(it) { onDismiss() } }
        2 -> {
            if (companyPick == null) {
                CompanyPicker(companies = companies, onDismiss = onDismiss, onPick = { companyPick = it })
            } else {
                ApplicationFormDialog(companyId = companyPick!!.id, onDismiss = onDismiss) { app ->
                    scope.launch {
                        val profile = vm.profileId()
                        vm.addApplication(app.copy(profileId = profile)) { onDismiss() }
                    }
                }
            }
        }
        3 -> {
            RoundPickerSheet(
                companies = companies,
                onDismiss = onDismiss,
                onPickApp = { appPick = it }
            )
            if (appPick != null) {
                RoundFormDialog(applicationId = appPick!!.id, onDismiss = onDismiss) { r ->
                    vm.addRound(r.copy(companyName = appPick!!.companyName, jobTitle = appPick!!.jobTitle)) { onDismiss() }
                }
            } else if (pickOpen) {
                // placeholder to keep composition stable
            }
        }
        4 -> ContactFormDialog(companyId = companies.firstOrNull()?.id, onDismiss = onDismiss) {
            vm.addContact(it) { onDismiss() }
        }
    }
}

@Composable
private fun CompanyPicker(
    companies: List<Company>,
    onDismiss: () -> Unit,
    onPick: (Company) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose company") },
        text = {
            Column {
                companies.forEach { c ->
                    Text(
                        c.name, modifier = Modifier.fillMaxWidth().padding(8.dp)
                            .clickableNoRipple { onPick(c) }
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RoundPickerSheet(
    companies: List<Company>,
    onDismiss: () -> Unit,
    onPickApp: (com.freedu.myinterviews.domain.model.JobApplication) -> Unit
) {
    // Simplified: round creation from quick-add needs an application context —
    // direct the user to open an application instead of duplicating the picker graph.
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add round") },
        text = { Text("Open an application from Pipeline, then use + Round there to get reminders and prep.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(indication = null, interactionSource = interaction, onClick = onClick)
}
