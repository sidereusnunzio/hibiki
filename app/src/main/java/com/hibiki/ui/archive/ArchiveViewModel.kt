package com.hibiki.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.ArchiveFilters
import com.hibiki.domain.model.PhraseListItem
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container()
    private val filters = MutableStateFlow(ArchiveFilters())

    val items: StateFlow<List<PhraseListItem>> = container.phraseRepository
        .observeArchive(filters)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val saved = container.settingsRepository.preferences.first()
            filters.value = saved.archiveFilters
            filters.collect { current ->
                container.settingsRepository.setArchiveFilters(current)
            }
        }
        viewModelScope.launch {
            items.collect { list ->
                container.archiveBrowseSession.updatePhraseIds(list.map { it.phrase.id })
            }
        }
    }

    val contexts: StateFlow<List<StudyContext>> = container.contextRepository
        .observeContexts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<Subject>> = filters
        .flatMapLatest { current ->
            val contextId = current.contextId
            if (contextId == null) {
                flowOf(emptyList())
            } else {
                container.contextRepository.observeSubjects(contextId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentFilters: StateFlow<ArchiveFilters> = filters.asStateFlow()

    fun setQuery(query: String) = filters.update { it.copy(query = query) }
    fun setContext(contextId: String) = filters.update { it.copy(contextId = contextId, subjectId = null) }
    fun setSubject(subjectId: String?) = filters.update { it.copy(subjectId = subjectId) }

    fun play(path: String) {
        container.phraseAudioPlayer.play(path)
    }
}
