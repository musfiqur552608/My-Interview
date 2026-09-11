package com.freedu.myinterviews.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: TrackerRepository
) : ViewModel() {
    val query = MutableStateFlow("")
    val companies: StateFlow<List<Company>> = repo.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<Contact>> =
        combine(repo.observeContacts(), query) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.name.contains(q, true) || it.role.contains(q, true) ||
                    it.email.contains(q, true) || it.companyName.contains(q, true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(contact: Contact) {
        viewModelScope.launch { repo.upsertContact(contact) }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch { repo.deleteContact(contact) }
    }
}
