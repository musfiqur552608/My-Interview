package com.freedu.myinterviews.presentation.prep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.data.local.entity.PrepTemplateEntity
import com.freedu.myinterviews.domain.model.PrepTemplate
import com.freedu.myinterviews.domain.model.QuestionBankItem
import com.freedu.myinterviews.domain.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class PrepViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val defaultTemplates: Provider<List<PrepTemplateEntity>>
) : ViewModel() {
    val query = MutableStateFlow("")

    val questions: StateFlow<List<QuestionBankItem>> =
        combine(repo.observeQuestions(), query) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.question.contains(q, true) || it.answer.contains(q, true) ||
                    it.tags.any { t -> t.contains(q, true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<PrepTemplate>> = repo.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Seed reusable prep templates once (offline, idempotent).
        viewModelScope.launch {
            val current = runCatching {
                repo.observeTemplates().first()
            }.getOrDefault(emptyList())
            if (current.isEmpty()) {
                defaultTemplates.get().forEach {
                    repo.upsertTemplate(PrepTemplate(name = it.name, items = it.itemsCsv.split(";;")))
                }
            }
        }
    }

    fun saveQuestion(item: QuestionBankItem) {
        viewModelScope.launch { repo.upsertQuestion(item) }
    }

    fun deleteQuestion(id: Long) {
        viewModelScope.launch { repo.deleteQuestion(id) }
    }

    fun saveTemplate(template: PrepTemplate) {
        viewModelScope.launch { repo.upsertTemplate(template) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repo.deleteTemplate(id) }
    }
}
