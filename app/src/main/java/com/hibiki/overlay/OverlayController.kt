package com.hibiki.overlay

import com.hibiki.data.repository.AudioCaptureRepository
import com.hibiki.data.repository.ContextRepository
import com.hibiki.data.repository.SettingsRepository
import com.hibiki.domain.CapturePipeline
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.OverlayStage
import com.hibiki.domain.model.OverlayUiState
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayController(
    private val contextRepository: ContextRepository,
    private val settingsRepository: SettingsRepository,
    private val audioCaptureRepository: AudioCaptureRepository,
    private val capturePipeline: CapturePipeline,
) {
    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    private var listenJob: Job? = null
    private var tickerJob: Job? = null
    private var bufferJob: Job? = null

    init {
        audioCaptureRepository.onBufferingDied = {
            _state.update { it.copy(bufferEnabled = false) }
        }
    }

    suspend fun hydrate() {
        val prefs = settingsRepository.preferences.first()
        val contexts = contextRepository.observeContexts().first()
        val selectedContext = contexts.find { it.id == prefs.lastContextId } ?: contexts.firstOrNull()
        val subjects = selectedContext?.takeIf { it.hasSubjects }
            ?.let { contextRepository.getOverlaySubjects(it.id) }
            .orEmpty()
        val lastSubjectId = selectedContext?.id?.let { prefs.lastSubjectIds[it] }
        val selectedSubject = lastSubjectId?.let { id -> subjects.find { it.id == id } }
        if (selectedContext != null && lastSubjectId != null && selectedSubject == null) {
            settingsRepository.setLastSubject(selectedContext.id, null)
        }
        _state.update {
            it.copy(
                collapsed = false,
                contexts = contexts,
                subjects = subjects,
                selectedContext = selectedContext,
                selectedSubject = selectedSubject,
                stage = OverlayStage.IDLE,
            )
        }
    }

    fun observeUpdates(scope: CoroutineScope) {
        scope.launch {
            contextRepository.observeContexts().collect { contexts ->
                val current = _state.value
                val selected = contexts.find { it.id == current.selectedContext?.id }
                    ?: contexts.firstOrNull()
                val contextChanged = selected?.id != current.selectedContext?.id ||
                    selected?.hasSubjects != current.selectedContext?.hasSubjects
                _state.update {
                    it.copy(
                        contexts = contexts,
                        selectedContext = selected,
                    )
                }
                if (selected != null && contextChanged) {
                    selectContext(selected, scope)
                }
            }
        }
        scope.launch {
            _state
                .map { it.selectedContext?.id to (it.selectedContext?.hasSubjects == true) }
                .distinctUntilChanged()
                .flatMapLatest { (id, hasSubjects) ->
                    if (id != null && hasSubjects) contextRepository.observeOverlaySubjects(id)
                    else flowOf(emptyList())
                }
                .collect { subjects ->
                    val current = _state.value.selectedSubject
                    val selected = current?.let { subject -> subjects.find { it.id == subject.id } }
                    if (selected == null && current != null) {
                        val contextId = _state.value.selectedContext?.id
                        if (contextId != null) {
                            scope.launch { settingsRepository.setLastSubject(contextId, null) }
                        }
                    }
                    _state.update { it.copy(subjects = subjects, selectedSubject = selected) }
                }
        }
        scope.launch {
            settingsRepository.preferences
                .map { it.audio.bufferDurationSeconds }
                .distinctUntilChanged()
                .collect { seconds ->
                    audioCaptureRepository.setBufferDurationMs(seconds * 1_000)
                }
        }
    }

    fun setCollapsed(collapsed: Boolean, scope: CoroutineScope) {
        _state.update { it.copy(collapsed = collapsed) }
        scope.launch { settingsRepository.setOverlayCollapsed(collapsed) }
    }

    fun selectContext(context: StudyContext, scope: CoroutineScope) {
        scope.launch {
            val subjects = if (context.hasSubjects) {
                contextRepository.getOverlaySubjects(context.id)
            } else {
                emptyList()
            }
            val prefs = settingsRepository.preferences.first()
            val selectedSubject = prefs.lastSubjectIds[context.id]?.let { id ->
                subjects.find { it.id == id }
            }
            settingsRepository.setLastContext(context.id)
            settingsRepository.setLastSubject(context.id, selectedSubject?.id)
            _state.update {
                it.copy(
                    selectedContext = context,
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                )
            }
        }
    }

    fun selectSubject(subject: Subject?, scope: CoroutineScope) {
        val contextId = _state.value.selectedContext?.id ?: return
        _state.update { it.copy(selectedSubject = subject) }
        scope.launch { settingsRepository.setLastSubject(contextId, subject?.id) }
    }

    fun setBufferEnabled(enabled: Boolean, scope: CoroutineScope) {
        if (_state.value.stage == OverlayStage.LISTENING) return
        if (_state.value.bufferEnabled == enabled) return
        bufferJob?.cancel()
        _state.update { it.copy(bufferEnabled = enabled) }
        bufferJob = scope.launch {
            try {
                if (enabled) {
                    val seconds = settingsRepository.preferences.first().audio.bufferDurationSeconds
                    audioCaptureRepository.startBuffering(seconds * 1_000)
                    if (!_state.value.bufferEnabled) {
                        audioCaptureRepository.stopBuffering()
                    }
                } else {
                    audioCaptureRepository.stopBuffering()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val appError = AppError.fromThrowable(error)
                _state.update {
                    it.copy(
                        bufferEnabled = false,
                        stage = OverlayStage.ERROR,
                        errorMessage = appError.userMessage,
                        remainingSeconds = null,
                    )
                }
            }
        }
    }

    fun startListen(scope: CoroutineScope) {
        if (_state.value.stage == OverlayStage.LISTENING) return
        val context = _state.value.selectedContext ?: return
        listenJob?.cancel()
        tickerJob?.cancel()
        listenJob = scope.launch {
            try {
                bufferJob?.join()
                val prefs = settingsRepository.preferences.first()
                _state.update {
                    it.copy(
                        stage = OverlayStage.LISTENING,
                        errorMessage = null,
                        result = null,
                        phrasePanelVisible = false,
                        remainingSeconds = prefs.audio.maxDurationSeconds,
                    )
                }
                tickerJob = launch {
                    var remaining = prefs.audio.maxDurationSeconds
                    while (isActive && remaining >= 0 && _state.value.stage == OverlayStage.LISTENING) {
                        _state.update { state -> state.copy(remainingSeconds = remaining) }
                        delay(1_000)
                        remaining--
                    }
                }
                val recorded = audioCaptureRepository.recordClip(
                    maxDurationMs = prefs.audio.maxDurationSeconds * 1_000L,
                    trimSilence = prefs.audio.trimSilence,
                )
                tickerJob?.cancel()
                val result = capturePipeline.process(
                    recorded = recorded,
                    context = context,
                    subject = if (context.hasSubjects) _state.value.selectedSubject else null,
                    onStage = { stage -> _state.update { it.copy(stage = stage, remainingSeconds = null) } },
                )
                _state.update {
                    it.copy(
                        stage = OverlayStage.RESULT,
                        result = result,
                        phrasePanelVisible = true,
                        remainingSeconds = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val appError = AppError.fromThrowable(error)
                _state.update {
                    it.copy(
                        stage = OverlayStage.ERROR,
                        errorMessage = appError.userMessage,
                        remainingSeconds = null,
                    )
                }
            }
        }
    }

    fun stopListen() {
        audioCaptureRepository.requestStop()
    }

    fun resetToIdle() {
        _state.update {
            it.copy(
                stage = OverlayStage.IDLE,
                errorMessage = null,
                remainingSeconds = null,
            )
        }
    }

    fun setPhrasePanelVisible(visible: Boolean) {
        _state.update { it.copy(phrasePanelVisible = visible) }
    }
}
