package com.hibiki.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.data.api.openai.OpenAiCostsClient
import com.hibiki.data.api.openai.OpenAiCostsReport
import com.hibiki.domain.model.ApiProviderId
import com.hibiki.domain.model.ApiSettings
import com.hibiki.domain.model.AppPreferences
import com.hibiki.domain.model.AudioSettings
import com.hibiki.domain.model.OverlayDisplayPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val adminApiKey: String = "",
    val persistedApiKey: String = "",
    val persistedAdminApiKey: String = "",
    val isApiKeyEditing: Boolean = false,
    val isAdminApiKeyEditing: Boolean = false,
    val providerId: String = ApiProviderId.OPENAI,
    val transcriptionModel: String = "gpt-transcribe",
    val analysisModel: String = "gpt-4o-mini",
    val persistState: PersistState = PersistState.Idle,
    val pingState: PingState = PingState.Idle,
    val costsState: CostsState = CostsState.Idle,
    val audio: AudioSettings = AudioSettings(),
    val overlayDisplay: OverlayDisplayPrefs = OverlayDisplayPrefs(),
)

sealed interface PersistState {
    data object Idle : PersistState
    data object Loading : PersistState
    data object Success : PersistState
    data object Error : PersistState
}

sealed interface PingState {
    data object Idle : PingState
    data object Loading : PingState
    data object Success : PingState
    data class Error(val message: String) : PingState
}

sealed interface CostsState {
    data object Idle : CostsState
    data object Loading : CostsState
    data class Success(val report: OpenAiCostsReport) : CostsState
    data class Error(val message: String) : CostsState
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container()
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.preferences.collect { prefs ->
                _uiState.update { current -> current.fromPrefs(prefs) }
            }
        }
    }

    fun setApiKey(value: String) {
        if (_uiState.value.apiKey == value) return
        _uiState.update {
            it.copy(
                apiKey = value,
                isApiKeyEditing = it.isApiKeyEditing || it.persistedApiKey.isBlank(),
                persistState = PersistState.Idle,
                pingState = PingState.Idle,
                costsState = CostsState.Idle,
            )
        }
        schedulePersistKeys()
    }

    fun setAdminApiKey(value: String) {
        if (_uiState.value.adminApiKey == value) return
        _uiState.update {
            it.copy(
                adminApiKey = value,
                isAdminApiKeyEditing = it.isAdminApiKeyEditing || it.persistedAdminApiKey.isBlank(),
                persistState = PersistState.Idle,
                costsState = CostsState.Idle,
            )
        }
        schedulePersistKeys()
    }

    fun unlockApiKeyEditing() {
        _uiState.update { it.copy(isApiKeyEditing = true) }
    }

    fun unlockAdminApiKeyEditing() {
        _uiState.update { it.copy(isAdminApiKeyEditing = true) }
    }

    fun setTranscriptionModel(value: String) = persistApi { it.copy(transcriptionModel = value) }
    fun setAnalysisModel(value: String) = persistApi { it.copy(languageAnalysisModel = value) }

    fun setMaxDuration(seconds: Int) {
        persistAudio { it.copy(maxDurationSeconds = seconds.coerceIn(2, 30)) }
    }

    fun setSaveAudio(value: Boolean) = persistAudio { it.copy(saveAudio = value) }
    fun setTrimSilence(value: Boolean) = persistAudio { it.copy(trimSilence = value) }
    fun setThreshold(value: Float) = persistAudio { it.copy(fingerprintThreshold = value) }

    fun setShowJapanese(value: Boolean) = persistDisplay { it.copy(showJapanese = value) }
    fun setShowKana(value: Boolean) = persistDisplay { it.copy(showKana = value) }
    fun setShowRomaji(value: Boolean) = persistDisplay { it.copy(showRomaji = value) }
    fun setShowLiteral(value: Boolean) = persistDisplay { it.copy(showLiteral = value) }
    fun setShowNatural(value: Boolean) = persistDisplay { it.copy(showNatural = value) }

    fun testConnection() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.apiKey.isBlank()) return@launch
            _uiState.update { it.copy(pingState = PingState.Loading) }
            try {
                persistKeysNow()
                container.languageAnalysisRepository.testConnection(
                    current.transcriptionModel,
                    current.analysisModel,
                )
                _uiState.update { it.copy(pingState = PingState.Success) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(pingState = PingState.Error(error.message ?: "Errore"))
                }
            }
        }
    }

    fun fetchCosts() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.adminApiKey.isBlank()) return@launch
            _uiState.update { it.copy(costsState = CostsState.Loading) }
            try {
                persistKeysNow()
                val report = OpenAiCostsClient(apiKey = current.adminApiKey.trim()).fetchLast28Days()
                _uiState.update { it.copy(costsState = CostsState.Success(report)) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(costsState = CostsState.Error(error.message ?: "Errore"))
                }
            }
        }
    }

    private fun schedulePersistKeys() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            persistKeysNow()
        }
    }

    private suspend fun persistKeysNow() {
        val current = _uiState.value
        try {
            _uiState.update { it.copy(persistState = PersistState.Loading) }
            container.settingsRepository.setApiKey(current.apiKey)
            container.settingsRepository.setAdminApiKey(current.adminApiKey)
            _uiState.update {
                it.copy(
                    persistState = PersistState.Success,
                    persistedApiKey = current.apiKey,
                    persistedAdminApiKey = current.adminApiKey,
                )
            }
        } catch (_: Throwable) {
            _uiState.update { it.copy(persistState = PersistState.Error) }
        }
    }

    private fun persistApi(block: (ApiSettings) -> ApiSettings) {
        val current = _uiState.value
        val next = block(
            ApiSettings(
                providerId = current.providerId,
                transcriptionModel = current.transcriptionModel,
                languageAnalysisModel = current.analysisModel,
                hasApiKey = current.persistedApiKey.isNotBlank(),
            ),
        )
        _uiState.update {
            it.copy(
                providerId = next.providerId,
                transcriptionModel = next.transcriptionModel,
                analysisModel = next.languageAnalysisModel,
                pingState = PingState.Idle,
            )
        }
        viewModelScope.launch { container.settingsRepository.setApiSettings(next) }
    }

    private fun persistAudio(block: (AudioSettings) -> AudioSettings) {
        val next = block(_uiState.value.audio)
        _uiState.update { it.copy(audio = next) }
        viewModelScope.launch { container.settingsRepository.setAudioSettings(next) }
    }

    private fun persistDisplay(block: (OverlayDisplayPrefs) -> OverlayDisplayPrefs) {
        val next = block(_uiState.value.overlayDisplay)
        _uiState.update { it.copy(overlayDisplay = next) }
        viewModelScope.launch { container.settingsRepository.setOverlayDisplay(next) }
    }

    private fun SettingsUiState.fromPrefs(prefs: AppPreferences): SettingsUiState {
        val storedKey = container.settingsRepository.getApiKey().orEmpty()
        val storedAdmin = container.settingsRepository.getAdminApiKey().orEmpty()
        return copy(
            persistedApiKey = storedKey,
            persistedAdminApiKey = storedAdmin,
            apiKey = if (isApiKeyEditing) apiKey else storedKey,
            adminApiKey = if (isAdminApiKeyEditing) adminApiKey else storedAdmin,
            providerId = prefs.api.providerId,
            transcriptionModel = prefs.api.transcriptionModel,
            analysisModel = prefs.api.languageAnalysisModel,
            audio = prefs.audio,
            overlayDisplay = prefs.overlayDisplay,
        )
    }

    companion object {
        private const val PERSIST_DEBOUNCE_MS = 400L
    }
}
