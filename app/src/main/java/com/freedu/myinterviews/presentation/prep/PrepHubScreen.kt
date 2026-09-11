package com.freedu.myinterviews.presentation.prep

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.freedu.myinterviews.domain.model.PrepTemplate
import com.freedu.myinterviews.domain.model.QuestionBankItem
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepHubScreen(vm: PrepViewModel = hiltViewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    val questions by vm.questions.collectAsState()
    val templates by vm.templates.collectAsState()
    val query by vm.query.collectAsState()
    var addQ by remember { mutableStateOf(false) }
    var editQ by remember { mutableStateOf<QuestionBankItem?>(null) }
    var addT by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Prep Hub", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (tab == 0) addQ = true else addT = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Question bank") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Templates") })
            }
            if (tab == 0) {
                SearchBar(query, { vm.query.value = it }, placeholder = "Search questions, answers, tags…",
                    modifier = Modifier.padding(16.dp))
                if (questions.isEmpty()) {
                    EmptyState(icon = Icons.Default.School, title = "No saved questions",
                        subtitle = "Store tricky questions and your best answers for reuse.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(questions, key = { it.id }) { q ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                                Row(Modifier.padding(14.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(q.question, fontWeight = FontWeight.SemiBold)
                                        if (q.answer.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(q.answer, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (q.tags.isNotEmpty()) {
                                            Text(q.tags.joinToString(" ") { "#$it" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { editQ = q }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { vm.deleteQuestion(q.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            } else {
                if (templates.isEmpty()) {
                    EmptyState(icon = Icons.Default.School, title = "No templates",
                        subtitle = "Create reusable checklists, e.g. Standard Technical Round.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(templates, key = { it.id }) { t ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                                Row(Modifier.padding(14.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.name, fontWeight = FontWeight.SemiBold)
                                        t.items.forEach { item ->
                                            Text("• $item", style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { vm.deleteTemplate(t.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
    if (addQ || editQ != null) {
        QuestionDialog(initial = editQ, onDismiss = { addQ = false; editQ = null }) {
            vm.saveQuestion(it); addQ = false; editQ = null
        }
    }
    if (addT) {
        TemplateDialog(onDismiss = { addT = false }) {
            vm.saveTemplate(it); addT = false
        }
    }
}

@Composable
private fun QuestionDialog(
    initial: QuestionBankItem?,
    onDismiss: () -> Unit,
    onSave: (QuestionBankItem) -> Unit
) {
    var q by remember { mutableStateOf(initial?.question.orEmpty()) }
    var a by remember { mutableStateOf(initial?.answer.orEmpty()) }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add question" else "Edit question") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(q, { q = it }, label = { Text("Question *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(a, { a = it }, label = { Text("Your best answer") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = q.isNotBlank(), onClick = {
                onSave((initial ?: QuestionBankItem(question = q.trim())).copy(
                    question = q.trim(), answer = a.trim(),
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TemplateDialog(onDismiss: () -> Unit, onSave: (PrepTemplate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var items by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New prep template") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Template name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(items, { items = it }, label = { Text("Items (one per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(PrepTemplate(name = name.trim(),
                    items = items.lines().map { it.trim() }.filter { it.isNotEmpty() }))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
