package com.hibiki.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhraseAdvancedEditUi(
    val phrase: Phrase,
    val japaneseEdit: String,
    val kana: String,
    val romaji: String,
    val literal: String,
    val natural: String,
    val contextId: String,
    val subjectId: String?,
    val arashiSyncState: ArashiSyncState,
    val saving: Boolean = false,
    val regenerating: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PhraseAdvancedEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    private val phraseId: String = checkNotNull(savedStateHandle["phraseId"])

    private val extra = MutableStateFlow<PhraseAdvancedEditUi?>(null)
    private val selectedContextId = MutableStateFlow<String?>(null)

    val state: StateFlow<PhraseAdvancedEditUi?> = extra.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val contexts: StateFlow<List<StudyContext>> = container.contextRepository
        .observeContexts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<Subject>> = selectedContextId
        .flatMapLatest { contextId ->
            if (contextId == null) {
                flowOf(emptyList())
            } else {
                container.contextRepository.observeSubjects(contextId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            container.phraseRepository.observePhrase(phraseId).filterNotNull().collect { phrase ->
                if (selectedContextId.value == null) {
                    selectedContextId.value = phrase.contextId
                }
                val current = extra.value
                extra.value = PhraseAdvancedEditUi(
                    phrase = phrase,
                    japaneseEdit = phrase.japaneseDisplay,
                    kana = phrase.kana,
                    romaji = phrase.romaji,
                    literal = phrase.literalTranslation,
                    natural = phrase.naturalTranslation,
                    contextId = current?.contextId ?: phrase.contextId,
                    subjectId = current?.subjectId ?: phrase.subjectId,
                    arashiSyncState = current?.arashiSyncState ?: phrase.arashiSyncState,
                    saving = false,
                    regenerating = false,
                    error = current?.error,
                )
            }
        }
    }

    fun updateJapanese(value: String) = extra.updateField { it.copy(japaneseEdit = value) }
    fun updateKana(value: String) = extra.updateField { it.copy(kana = value) }
    fun updateRomaji(value: String) = extra.updateField { it.copy(romaji = value) }
    fun updateLiteral(value: String) = extra.updateField { it.copy(literal = value) }
    fun updateNatural(value: String) = extra.updateField { it.copy(natural = value) }

    fun setContext(contextId: String) {
        selectedContextId.value = contextId
        extra.updateField { it.copy(contextId = contextId, subjectId = null) }
    }

    fun setSubject(subjectId: String?) = extra.updateField { it.copy(subjectId = subjectId) }

    fun setArashiSyncState(state: ArashiSyncState) = extra.updateField { it.copy(arashiSyncState = state) }

    fun save() {
        val current = extra.value ?: return
        viewModelScope.launch {
            extra.value = current.copy(saving = true, error = null)
            runCatching {
                val corrected = current.japaneseEdit.trim()
                    .takeIf { it.isNotBlank() && it != current.phrase.japaneseRaw }
                val selectedContext = contexts.value.find { it.id == current.contextId }
                val subjectId = if (selectedContext?.hasSubjects == true) current.subjectId else null
                container.phraseRepository.updateManualFields(
                    current.phrase.copy(
                        contextId = current.contextId,
                        subjectId = subjectId,
                        japaneseCorrected = corrected,
                        kana = current.kana,
                        romaji = current.romaji,
                        literalTranslation = current.literal,
                        naturalTranslation = current.natural,
                        arashiSyncState = current.arashiSyncState,
                    ),
                )
            }.onFailure { error ->
                extra.value = extra.value?.copy(
                    saving = false,
                    error = AppError.fromThrowable(error).userMessage,
                )
            }
        }
    }

    fun regenerate() {
        val current = extra.value ?: return
        viewModelScope.launch {
            extra.value = current.copy(regenerating = true, error = null)
            try {
                val context = container.contextRepository.getContext(current.contextId)
                    ?: error("Contesto non trovato")
                val subject = current.subjectId?.let { container.contextRepository.getSubject(it) }
                    .takeIf { context.hasSubjects }
                val analysis = container.capturePipeline.reanalyze(
                    japanese = current.japaneseEdit.trim(),
                    context = context,
                    subject = subject,
                )
                val corrected = current.japaneseEdit.trim()
                    .takeIf { it.isNotBlank() && it != current.phrase.japaneseRaw }
                container.phraseRepository.updateLinguisticFields(phraseId, analysis, corrected)
            } catch (error: Throwable) {
                extra.value = extra.value?.copy(
                    regenerating = false,
                    error = AppError.fromThrowable(error).userMessage,
                )
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { container.phraseRepository.delete(phraseId) }
                .onSuccess { onDeleted() }
                .onFailure { error ->
                    extra.value = extra.value?.copy(
                        error = AppError.fromThrowable(error).userMessage,
                    )
                }
        }
    }

    private fun MutableStateFlow<PhraseAdvancedEditUi?>.updateField(block: (PhraseAdvancedEditUi) -> PhraseAdvancedEditUi) {
        value = value?.let(block)
    }
}
