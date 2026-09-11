package com.freedu.myinterviews.presentation.pipeline

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.presentation.components.CompanyAvatar
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.SearchBar
import com.freedu.myinterviews.presentation.components.StatusChip
import com.freedu.myinterviews.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PipelineScreen(
    onOpenApplication: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    vm: PipelineViewModel = hiltViewModel()
) {
    val apps by vm.applications.collectAsState()
    val query by vm.query.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val sortBy by vm.sortBy.collectAsState()
    val notice by vm.notice.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        if (notice != null) { snack.showSnackbar(notice!!); vm.clearNotice() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        vm.viewMode.value =
                            if (viewMode == ViewMode.KANBAN) ViewMode.LIST else ViewMode.KANBAN
                    }) {
                        Icon(
                            if (viewMode == ViewMode.KANBAN) Icons.Default.ViewList else Icons.Default.ViewKanban,
                            contentDescription = "Toggle view"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onQuickAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add application")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SearchBar(query, vm::setQuery, modifier = Modifier.padding(horizontal = 16.dp))
            // Status filter chips
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.width(8.dp)) }
                item {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { vm.statusFilter.value = null },
                        label = { Text("All (${apps.size})") }
                    )
                }
                items(ApplicationStatus.values().toList(), key = { it.name }) { s ->
                    val count = apps.count { it.status == s }
                    FilterChip(
                        selected = statusFilter == s,
                        onClick = { vm.statusFilter.value = if (statusFilter == s) null else s },
                        label = { Text("${s.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ($count)") }
                    )
                }
                item {
                    var sortOpen by remember { mutableStateOf(false) }
                    FilterChip(selected = false, onClick = { sortOpen = true },
                        label = { Text("Sort: ${sortBy.name.lowercase()}") })
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                        SortBy.values().forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name.lowercase()) },
                                onClick = { vm.sortBy.value = s; sortOpen = false }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.width(8.dp)) }
            }
            if (apps.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Work,
                    title = "No applications here",
                    subtitle = "Tap + to add your first application, or clear filters."
                )
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.id }) { app ->
                        ApplicationCard(app, onOpenApplication, onMove = { vm.move(app, it) })
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            } else {
                // Kanban: horizontally scrollable status columns; move via dialog (accessible
                // alternative to drag-and-drop that also works with screen readers).
                LazyRow(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.width(4.dp)) }
                    items(ApplicationStatus.values().toList(), key = { it.name }) { status ->
                        val col = apps.filter { it.status == status }
                        KanbanColumn(
                            status = status,
                            apps = col,
                            onOpen = onOpenApplication,
                            onMove = { vm.move(it, status) },
                            onMoveTo = { app, to -> vm.move(app, to) }
                        )
                    }
                    item { Spacer(Modifier.width(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    app: JobApplication,
    onOpen: (Long) -> Unit,
    onMove: (ApplicationStatus) -> Unit
) {
    var moveOpen by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onOpen(app.id) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            CompanyAvatar(app.companyName.ifBlank { app.jobTitle })
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(app.jobTitle, fontWeight = FontWeight.SemiBold)
                Text(
                    "${app.companyName} · ${DateUtils.date(app.appliedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.padding(top = 6.dp)) {
                    StatusChip(app.status)
                    if (app.salaryMax > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${app.salaryMin / 1000}k–${app.salaryMax / 1000}k",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { moveOpen = true }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Change status")
            }
        }
    }
    if (moveOpen) {
        MoveDialog(current = app.status, onDismiss = { moveOpen = false }) {
            onMove(it); moveOpen = false
        }
    }
}

@Composable
private fun KanbanColumn(
    status: ApplicationStatus,
    apps: List<JobApplication>,
    onOpen: (Long) -> Unit,
    onMove: (JobApplication) -> Unit,
    onMoveTo: (JobApplication, ApplicationStatus) -> Unit
) {
    Column(Modifier.width(280.dp)) {
        Text(
            "${status.name.lowercase().replaceFirstChar { it.uppercase() }} (${apps.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        apps.forEach { app ->
            var moveOpen by remember { mutableStateOf(false) }
            Card(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpen(app.id) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(app.jobTitle, fontWeight = FontWeight.SemiBold)
                    Text(app.companyName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 6.dp)) {
                        StatusChip(app.status)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { moveOpen = true }, modifier = Modifier.padding(0.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Move",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (moveOpen) {
                MoveDialog(current = app.status, onDismiss = { moveOpen = false }) {
                    onMoveTo(app, it); moveOpen = false
                }
            }
        }
        if (apps.isEmpty()) {
            Text("Drop here —", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun MoveDialog(
    current: ApplicationStatus,
    onDismiss: () -> Unit,
    onPick: (ApplicationStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to stage") },
        text = {
            Column {
                ApplicationStatus.values().forEach { s ->
                    TextButton(
                        onClick = { onPick(s) },
                        enabled = s != current,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            (if (s == current) "● " else "") +
                                s.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
