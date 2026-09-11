package com.freedu.myinterviews.presentation.contacts

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.presentation.company.ContactFormDialog
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(vm: ContactsViewModel = hiltViewModel()) {
    val contacts by vm.contacts.collectAsState()
    val query by vm.query.collectAsState()
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Contact?>(null) }
    var del by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Contacts", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { add = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SearchBar(query, { vm.query.value = it }, placeholder = "Search recruiters, interviewers…",
                modifier = Modifier.padding(horizontal = 16.dp))
            if (contacts.isEmpty()) {
                EmptyState(icon = Icons.Default.Person, title = "No contacts yet",
                    subtitle = "Save recruiters and interviewers with notes and LinkedIn links.")
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(contacts, key = { it.id }) { c ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.padding(14.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                    if (c.role.isNotBlank() || c.companyName.isNotBlank()) {
                                        Text(listOf(c.role, c.companyName).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (c.email.isNotBlank()) Text(c.email, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { edit = c }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { del = c }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.width(88.dp)) }
                }
            }
        }
    }
    if (add) {
        ContactFormDialog(onDismiss = { add = false }) { vm.save(it); add = false }
    }
    if (edit != null) {
        ContactFormDialog(initial = edit, onDismiss = { edit = null }) { vm.save(it); edit = null }
    }
    if (del != null) {
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("Delete ${del!!.name}?") },
            text = { Text("This contact will be removed.") },
            confirmButton = { TextButton(onClick = { vm.delete(del!!); del = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { del = null }) { Text("Cancel") } }
        )
    }
}
