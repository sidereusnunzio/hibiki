package com.hibiki.ui.contexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SubjectDraft(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String = "",
    val japaneseName: String = "",
    val prompt: String = "",
)

data class ContextEditState(
    val id: String? = null,
    val name: String = "",
    val prompt: String = DefaultPrompts.GENERAL,
    val expectedLanguage: String = "ja",
    val hasSubjects: Boolean = false,
    val isBuiltIn: Boolean = false,
    val subjects: List<SubjectDraft> = emptyList(),
    val error: String? = null,
)

class ContextEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    private val contextId: String? = savedStateHandle["contextId"]
    private val _state = MutableStateFlow(ContextEditState(id = contextId))
    val state: StateFlow<ContextEditState> = _state.asStateFlow()
    private var persistedSubjectIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val id = contextId
            if (!id.isNullOrBlank()) {
                container.contextRepository.getContext(id)?.let { load(it) }
            }
        }
    }

    fun setName(value: String) = _state.updateCopy { copy(name = value) }
    fun setPrompt(value: String) = _state.updateCopy { copy(prompt = value) }
    fun setLanguage(value: String) = _state.updateCopy { copy(expectedLanguage = value) }
    fun setHasSubjects(value: Boolean) = _state.updateCopy { copy(hasSubjects = value) }

    fun addSubject() {
        _state.updateCopy {
            copy(
                hasSubjects = true,
                subjects = subjects + SubjectDraft(),
            )
        }
    }

    fun removeSubject(id: String) {
        _state.updateCopy { copy(subjects = subjects.filterNot { it.id == id }) }
    }

    fun setSubjectDisplayName(id: String, value: String) =
        updateSubject(id) { copy(displayName = value) }

    fun setSubjectJapaneseName(id: String, value: String) =
        updateSubject(id) { copy(japaneseName = value) }

    fun setSubjectPrompt(id: String, value: String) =
        updateSubject(id) { copy(prompt = value) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            if (current.name.isBlank()) {
                _state.value = current.copy(error = "Nome obbligatorio")
                return@launch
            }
            val drafts = current.subjects.filter { draft ->
                draft.displayName.isNotBlank() || draft.japaneseName.isNotBlank() || draft.prompt.isNotBlank()
            }
            drafts.firstOrNull { it.displayName.isBlank() || it.japaneseName.isBlank() }?.let {
                _state.value = current.copy(error = "Ogni personaggio deve avere nome rōmaji e nome giapponese")
                return@launch
            }
            try {
                val saved = if (current.id == null) {
                    container.contextRepository.createContext(
                        name = current.name,
                        prompt = current.prompt,
                        expectedLanguage = current.expectedLanguage,
                        hasSubjects = current.hasSubjects || drafts.isNotEmpty(),
                    )
                } else {
                    val existing = container.contextRepository.getContext(current.id) ?: return@launch
                    val updated = existing.copy(
                        name = current.name.trim(),
                        prompt = current.prompt.trim(),
                        expectedLanguage = current.expectedLanguage.trim().ifBlank { "ja" },
                        hasSubjects = current.hasSubjects || drafts.isNotEmpty(),
                    )
                    container.contextRepository.updateContext(updated)
                    updated
                }
                persistSubjects(saved.id, drafts)
                onDone()
            } catch (error: Throwable) {
                _state.value = current.copy(error = error.message)
            }
        }
    }

    private suspend fun persistSubjects(contextId: String, drafts: List<SubjectDraft>) {
        val keepIds = drafts.map { it.id }.toSet()
        persistedSubjectIds.filterNot { it in keepIds }.forEach { id ->
            container.contextRepository.deleteSubject(id)
        }
        drafts.forEach { draft ->
            container.contextRepository.saveSubject(
                Subject(
                    id = draft.id,
                    contextId = contextId,
                    displayName = draft.displayName.trim(),
                    japaneseName = draft.japaneseName.trim(),
                    prompt = draft.prompt.trim(),
                ),
            )
        }
    }

    private suspend fun load(context: StudyContext) {
        val subjects = container.contextRepository.getSubjects(context.id)
        persistedSubjectIds = subjects.map { it.id }.toSet()
        _state.value = ContextEditState(
            id = context.id,
            name = context.name,
            prompt = context.prompt,
            expectedLanguage = context.expectedLanguage,
            hasSubjects = context.hasSubjects,
            isBuiltIn = context.isBuiltIn,
            subjects = subjects.map { subject ->
                SubjectDraft(
                    id = subject.id,
                    displayName = subject.displayName,
                    japaneseName = subject.japaneseName,
                    prompt = subject.prompt,
                )
            },
        )
    }

    private fun updateSubject(id: String, block: SubjectDraft.() -> SubjectDraft) {
        _state.updateCopy {
            copy(subjects = subjects.map { draft -> if (draft.id == id) draft.block() else draft })
        }
    }

    private fun MutableStateFlow<ContextEditState>.updateCopy(block: ContextEditState.() -> ContextEditState) {
        value = value.block()
    }
}
