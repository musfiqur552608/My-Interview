package com.freedu.myinterviews.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.domain.repository.SearchResults
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.SearchBar
import com.freedu.myinterviews.presentation.components.SectionHeader
import com.freedu.myinterviews.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(repo: TrackerRepository) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results = query.debounce(250).flatMapLatest { repo.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenCompany: (Long) -> Unit,
    onOpenApplication: (Long) -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SearchBar(query, { vm.query.value = it }, modifier = Modifier.padding(horizontal = 16.dp))
            val empty = results.companies.isEmpty() && results.applications.isEmpty() &&
                results.rounds.isEmpty() && results.contacts.isEmpty()
            if (query.isBlank()) {
                EmptyState(icon = Icons.Default.Search, title = "Global search",
                    subtitle = "Find companies, roles, notes, interviewers and contacts.")
            } else if (empty) {
                EmptyState(icon = Icons.Default.Search, title = "No matches",
                    subtitle = "Try a different keyword.")
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (results.companies.isNotEmpty()) {
                        item { SectionHeader("Companies") }
                        items(results.companies, key = { "c${it.id}" }) { c ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                                .clickable { onOpenCompany(c.id) }, shape = RoundedCornerShape(12.dp)) {
                                Text(c.name, Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (results.applications.isNotEmpty()) {
                        item { SectionHeader("Applications") }
                        items(results.applications, key = { "a${it.id}" }) { a ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                                .clickable { onOpenApplication(a.id) }, shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(a.jobTitle, fontWeight = FontWeight.SemiBold)
                                    Text("${a.companyName} · ${a.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (results.rounds.isNotEmpty()) {
                        item { SectionHeader("Rounds & notes") }
                        items(results.rounds, key = { "r${it.id}" }) { r ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                                .clickable { onOpenApplication(r.applicationId) }, shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("${r.roundType} · ${r.companyName}", fontWeight = FontWeight.SemiBold)
                                    Text(DateUtils.dateTime(r.scheduledAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (results.contacts.isNotEmpty()) {
                        item { SectionHeader("Contacts") }
                        items(results.contacts, key = { "ct${it.id}" }) { c ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                    Text(listOf(c.role, c.companyName).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall)
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
