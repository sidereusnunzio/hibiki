package com.hibiki.ui.contexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.StudyContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContextsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container()

    val contexts: StateFlow<List<StudyContext>> = container.contextRepository
        .observeContexts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { container.contextRepository.deleteContext(id) }
                .onFailure { onError(it.message ?: "Impossibile eliminare") }
        }
    }
}
