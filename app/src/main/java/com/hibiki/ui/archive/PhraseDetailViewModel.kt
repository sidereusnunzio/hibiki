package com.hibiki.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhraseDetailUi(
    val phrase: Phrase,
    val contextName: String,
    val subject: Subject?,
    val japaneseEdit: String,
    val kana: String,
    val romaji: String,
    val literal: String,
    val natural: String,
    val saving: Boolean = false,
    val regenerating: Boolean = false,
    val error: String? = null,
    val createdLabel: String,
)

class PhraseDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    private val phraseId: String = checkNotNull(savedStateHandle["phraseId"])
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val extra = MutableStateFlow<PhraseDetailUi?>(null)

    val state: StateFlow<PhraseDetailUi?> = extra.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    init {
        viewModelScope.launch {
            container.phraseRepository.observePhrase(phraseId).filterNotNull().collect { phrase ->
                val context = container.contextRepository.getContext(phrase.contextId)
                val subject = phrase.subjectId?.let { container.contextRepository.getSubject(it) }
                extra.value = PhraseDetailUi(
                    phrase = phrase,
                    contextName = context?.name ?: phrase.contextId,
                    subject = subject,
                    japaneseEdit = phrase.japaneseDisplay,
                    kana = phrase.kana,
                    romaji = phrase.romaji,
                    literal = phrase.literalTranslation,
                    natural = phrase.naturalTranslation,
                    createdLabel = formatter.format(Date(phrase.createdAt)),
                )
            }
        }
    }

    fun updateJapanese(value: String) = extra.updateField { it.copy(japaneseEdit = value) }
    fun updateKana(value: String) = extra.updateField { it.copy(kana = value) }
    fun updateRomaji(value: String) = extra.updateField { it.copy(romaji = value) }
    fun updateLiteral(value: String) = extra.updateField { it.copy(literal = value) }
    fun updateNatural(value: String) = extra.updateField { it.copy(natural = value) }

    fun play() {
        extra.value?.phrase?.audioPath?.let { container.phraseAudioPlayer.play(it) }
    }

    fun setVerified(verified: Boolean) {
        viewModelScope.launch { container.phraseRepository.setVerified(phraseId, verified) }
    }

    fun save() {
        val current = extra.value ?: return
        viewModelScope.launch {
            extra.value = current.copy(saving = true, error = null)
            val corrected = current.japaneseEdit.trim()
                .takeIf { it.isNotBlank() && it != current.phrase.japaneseRaw }
            container.phraseRepository.updateManualFields(
                current.phrase.copy(
                    japaneseCorrected = corrected,
                    kana = current.kana,
                    romaji = current.romaji,
                    literalTranslation = current.literal,
                    naturalTranslation = current.natural,
                ),
            )
        }
    }

    fun regenerate() {
        val current = extra.value ?: return
        viewModelScope.launch {
            extra.value = current.copy(regenerating = true, error = null)
            try {
                val context = container.contextRepository.getContext(current.phrase.contextId)
                val analysis = container.capturePipeline.reanalyze(
                    japanese = current.japaneseEdit.trim(),
                    context = context,
                    subject = current.subject,
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

    private fun MutableStateFlow<PhraseDetailUi?>.updateField(block: (PhraseDetailUi) -> PhraseDetailUi) {
        value = value?.let(block)
    }
}
