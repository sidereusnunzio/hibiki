package com.hibiki.ui.contexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    val contextId: String = checkNotNull(savedStateHandle["contextId"])
    private val query = MutableStateFlow("")

    val subjects: StateFlow<List<Subject>> = query
        .flatMapLatest { container.contextRepository.observeSubjects(contextId, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val search: StateFlow<String> = query

    fun setQuery(value: String) {
        query.value = value
    }

    fun delete(id: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { container.contextRepository.deleteSubject(id) }
                .onFailure { onError(it.message ?: "Impossibile eliminare") }
        }
    }
}
