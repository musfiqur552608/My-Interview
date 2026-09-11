package com.freedu.myinterviews.presentation.company

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.presentation.components.CompanyAvatar
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.StatusChip
import com.freedu.myinterviews.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    onBack: () -> Unit,
    onOpenApplication: (Long) -> Unit,
    vm: CompanyViewModel = hiltViewModel()
) {
    val company by vm.company.collectAsState()
    val apps by vm.companyApps.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var editCompany by remember { mutableStateOf<Company?>(null) }
    var addApp by remember { mutableStateOf(false) }
    var editApp by remember { mutableStateOf<JobApplication?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(company?.name ?: "Company", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (company != null) {
                        IconButton(onClick = { editCompany = company }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit company")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete company")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (tab == 1 && company != null) {
                ExtendedFloatingActionButton(
                    onClick = { addApp = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Application") }
                )
            }
        }
    ) { pad ->
        val c = company
        if (c == null) {
            Column(Modifier.padding(pad).fillMaxSize().padding(24.dp)) {
                Text("Company not found. It may have been deleted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.padding(pad).fillMaxSize()) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Overview") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Applications (${apps.size})") })
                }
                if (tab == 0) {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(Modifier.padding(16.dp)) {
                                CompanyAvatar(c.name, Modifier.padding(end = 12.dp), accent = c.accentColor)
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    if (c.industry.isNotBlank()) Text(c.industry, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (c.website.isNotBlank()) Text(c.website, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (c.size.isNotBlank() || c.funding.isNotBlank() || c.difficulty.isNotBlank() || c.rating > 0) {
                            item {
                                DossierCard(c)
                            }
                        }
                        if (c.tags.isNotEmpty()) {
                            item {
                                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    c.tags.forEach { tag ->
                                        androidx.compose.material3.AssistChip(onClick = {}, label = { Text("#$tag") })
                                    }
                                }
                            }
                        }
                        if (c.source.isNotBlank()) {
                            item { Text("Source: ${c.source}", modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium) }
                        }
                        if (c.notes.isNotBlank()) {
                            item {
                                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                                    Text(c.notes, Modifier.padding(16.dp))
                                }
                            }
                        }
                        item {
                            Text("Applied ${DateUtils.date(c.createdAt)}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    if (apps.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Work, title = "No applications yet",
                            subtitle = "Add the role you applied for at ${c.name}."
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Spacer(Modifier.height(8.dp)) }
                            items(apps, key = { it.id }) { app ->
                                Card(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                        .clickable { onOpenApplication(app.id) },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row {
                                            Column(Modifier.weight(1f)) {
                                                Text(app.jobTitle, fontWeight = FontWeight.SemiBold)
                                                Text("${app.location.ifBlank { "—" }} · ${DateUtils.date(app.appliedDate)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            StatusChip(app.status)
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(88.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (editCompany != null) {
        CompanyFormDialog(initial = editCompany, onDismiss = { editCompany = null }) {
            vm.saveCompany(it); editCompany = null
        }
    }
    if (addApp && company != null) {
        ApplicationFormDialog(companyId = company!!.id, onDismiss = { addApp = false }) {
            vm.saveApplication(it); addApp = false
        }
    }
    if (editApp != null) {
        ApplicationFormDialog(companyId = editApp!!.companyId, initial = editApp, onDismiss = { editApp = null }) {
            vm.saveApplication(it); editApp = null
        }
    }
    if (confirmDelete && company != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${company!!.name}?") },
            text = { Text("All its applications and rounds will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCompany(company!!.id) { onBack() } }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

/** Company dossier: size, funding, difficulty, rating — edit from the header ✎. */
@Composable
private fun DossierCard(c: Company) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Company dossier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (c.rating > 0) Text("★ ${"%.1f".format(c.rating)}/5 employee rating",
                style = MaterialTheme.typography.bodyMedium)
            if (c.size.isNotBlank()) Text("Size: ${c.size} employees",
                style = MaterialTheme.typography.bodyMedium)
            if (c.funding.isNotBlank()) Text("Funding: ${c.funding}",
                style = MaterialTheme.typography.bodyMedium)
            if (c.difficulty.isNotBlank()) Text("Interview difficulty: ${c.difficulty}",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
