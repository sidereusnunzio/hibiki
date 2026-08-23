package com.hibiki.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhraseDetailUi(
    val phrase: Phrase,
    val contextName: String,
    val contextPrompt: String,
    val contextImagePath: String?,
    val subject: Subject?,
    val subjectImagePath: String?,
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

@OptIn(ExperimentalCoroutinesApi::class)
class PhraseDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = application.container()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val _currentPhraseId = MutableStateFlow(
        checkNotNull(savedStateHandle.get<String>("phraseId")),
    )
    val currentPhraseId: StateFlow<String> = _currentPhraseId.asStateFlow()

    val browsePhraseIds: StateFlow<List<String>> = container.archiveBrowseSession.phraseIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _detailCache = MutableStateFlow<Map<String, PhraseDetailUi>>(emptyMap())
    val detailCache: StateFlow<Map<String, PhraseDetailUi>> = _detailCache.asStateFlow()

    val state: StateFlow<PhraseDetailUi?> = combine(_currentPhraseId, _detailCache) { id, cache ->
        cache[id]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val neighborJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            combine(_currentPhraseId, browsePhraseIds) { id, ids ->
                neighborPhraseIds(id, ids)
            }
                .distinctUntilChanged()
                .collect { neighborIds ->
                    syncNeighborObservers(neighborIds)
                }
        }
    }

    fun showBrowsePhrase(phraseId: String) {
        if (phraseId == _currentPhraseId.value) return
        _currentPhraseId.value = phraseId
        savedStateHandle["phraseId"] = phraseId
    }

    fun openRandomPhrase(onNavigate: (String) -> Unit) {
        val phraseId = _currentPhraseId.value
        viewModelScope.launch {
            when (val pick = container.archiveBrowseSession.randomPhraseId(phraseId)) {
                is BrowseSessionRandomPick.Id -> onNavigate(pick.id)
                BrowseSessionRandomPick.StayInSession -> Unit
                BrowseSessionRandomPick.NotInSession -> {
                    container.phraseRepository.getAll()
                        .map { it.id }
                        .filter { it != phraseId }
                        .randomOrNull()
                        ?.let(onNavigate)
                }
            }
        }
    }

    fun updateJapanese(value: String) = updateCurrent { it.copy(japaneseEdit = value) }
    fun updateKana(value: String) = updateCurrent { it.copy(kana = value) }
    fun updateRomaji(value: String) = updateCurrent { it.copy(romaji = value) }
    fun updateLiteral(value: String) = updateCurrent { it.copy(literal = value) }
    fun updateNatural(value: String) = updateCurrent { it.copy(natural = value) }

    fun play() {
        state.value?.phrase?.audioPath?.let { container.phraseAudioPlayer.play(it) }
    }

    fun setVerified(verified: Boolean) {
        val phraseId = _currentPhraseId.value
        viewModelScope.launch { container.phraseRepository.setVerified(phraseId, verified) }
    }

    fun save() {
        val current = state.value ?: return
        viewModelScope.launch {
            updateCurrent { it.copy(saving = true, error = null) }
            runCatching {
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
            }.onFailure { error ->
                updateCurrent {
                    it.copy(
                        saving = false,
                        error = AppError.fromThrowable(error).userMessage,
                    )
                }
            }
        }
    }

    fun regenerate() {
        val current = state.value ?: return
        val phraseId = _currentPhraseId.value
        viewModelScope.launch {
            updateCurrent { it.copy(regenerating = true, error = null) }
            try {
                val context = container.contextRepository.getContext(current.phrase.contextId)
                    ?: error("Contesto non trovato")
                val analysis = container.capturePipeline.reanalyze(
                    japanese = current.japaneseEdit.trim(),
                    context = context,
                    subject = current.subject,
                )
                val corrected = current.japaneseEdit.trim()
                    .takeIf { it.isNotBlank() && it != current.phrase.japaneseRaw }
                container.phraseRepository.updateLinguisticFields(phraseId, analysis, corrected)
            } catch (error: Throwable) {
                updateCurrent {
                    it.copy(
                        regenerating = false,
                        error = AppError.fromThrowable(error).userMessage,
                    )
                }
            }
        }
    }

    private fun updateCurrent(block: (PhraseDetailUi) -> PhraseDetailUi) {
        val id = _currentPhraseId.value
        _detailCache.update { cache ->
            cache[id]?.let { cache + (id to block(it)) } ?: cache
        }
    }

    private fun syncNeighborObservers(neighborIds: List<String>) {
        val keep = neighborIds.toSet()
        neighborJobs.keys.filterNot { it in keep }.forEach { staleId ->
            neighborJobs.remove(staleId)?.cancel()
        }
        neighborIds.forEach { phraseId ->
            if (neighborJobs.containsKey(phraseId)) return@forEach
            neighborJobs[phraseId] = viewModelScope.launch {
                container.phraseRepository.observePhrase(phraseId).filterNotNull().collect { phrase ->
                    _detailCache.update { cache ->
                        cache + (phraseId to buildDetailUi(phrase))
                    }
                }
            }
        }
        val retained = keep + _currentPhraseId.value
        if (_detailCache.value.keys.any { it !in retained }) {
            _detailCache.value = _detailCache.value.filterKeys { it in retained }
        }
    }

    private fun neighborPhraseIds(currentId: String, ids: List<String>): List<String> {
        val index = ids.indexOf(currentId)
        if (index < 0) return listOf(currentId)
        return buildList {
            ids.getOrNull(index - 1)?.let(::add)
            add(currentId)
            ids.getOrNull(index + 1)?.let(::add)
        }
    }

    private suspend fun buildDetailUi(phrase: Phrase): PhraseDetailUi {
        val context = container.contextRepository.getContext(phrase.contextId)
        val subject = phrase.subjectId?.let { container.contextRepository.getSubject(it) }
        return PhraseDetailUi(
            phrase = phrase,
            contextName = context?.name ?: phrase.contextId,
            contextPrompt = context?.prompt.orEmpty(),
            contextImagePath = context?.imagePath,
            subject = subject,
            subjectImagePath = subject?.imagePath,
            japaneseEdit = phrase.japaneseDisplay,
            kana = phrase.kana,
            romaji = phrase.romaji,
            literal = phrase.literalTranslation,
            natural = phrase.naturalTranslation,
            saving = false,
            regenerating = false,
            error = null,
            createdLabel = formatter.format(Date(phrase.createdAt)),
        )
    }
}
