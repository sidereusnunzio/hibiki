package com.hibiki.ui.contexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubjectEditState(
    val id: String = "",
    val displayName: String = "",
    val japaneseName: String = "",
    val prompt: String = "",
    val error: String? = null,
)

class SubjectEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    val contextId: String = checkNotNull(savedStateHandle["contextId"])
    private val subjectId: String? = savedStateHandle["subjectId"]
    private val _state = MutableStateFlow(SubjectEditState(id = subjectId.orEmpty()))
    val state: StateFlow<SubjectEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val id = subjectId
            if (!id.isNullOrBlank()) {
                container.contextRepository.getSubject(id)?.let { subject ->
                    _state.value = SubjectEditState(
                        id = subject.id,
                        displayName = subject.displayName,
                        japaneseName = subject.japaneseName,
                        prompt = subject.prompt,
                    )
                }
            }
        }
    }

    fun setDisplayName(value: String) {
        _state.value = _state.value.copy(displayName = value)
    }

    fun setJapaneseName(value: String) {
        _state.value = _state.value.copy(japaneseName = value)
    }

    fun setPrompt(value: String) {
        _state.value = _state.value.copy(prompt = value)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            if (current.displayName.isBlank() || current.japaneseName.isBlank()) {
                _state.value = current.copy(error = "Nome e nome giapponese obbligatori")
                return@launch
            }
            container.contextRepository.saveSubject(
                Subject(
                    id = current.id,
                    contextId = contextId,
                    displayName = current.displayName.trim(),
                    japaneseName = current.japaneseName.trim(),
                    prompt = current.prompt.trim(),
                ),
            )
            onDone()
        }
    }
}
