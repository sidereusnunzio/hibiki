package com.hibiki.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.data.arashi.ArashiActivityResult
import com.hibiki.data.arashi.ArashiSyncOutcome
import com.hibiki.data.api.openai.OpenAiCostsClient
import com.hibiki.data.api.openai.OpenAiCostsReport
import com.hibiki.data.backup.BackupExportState
import com.hibiki.data.backup.BackupImportState
import com.hibiki.domain.model.ApiProviderId
import com.hibiki.domain.model.ApiSettings
import com.hibiki.domain.model.AppPreferences
import com.hibiki.domain.model.AudioBufferConfig
import com.hibiki.domain.model.AudioSettings
import com.hibiki.domain.model.LastArashiExport
import com.hibiki.domain.model.OverlayDisplayPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    val backupState: BackupState = BackupState.Idle,
    val lastArashiExport: LastArashiExport? = null,
    val arashiExportState: ArashiExportState = ArashiExportState.Idle,
)

sealed interface ArashiExportState {
    data object Idle : ArashiExportState
    data object Preparing : ArashiExportState
    data class Success(val summary: String) : ArashiExportState
    data class Error(val message: String) : ArashiExportState
}

sealed interface BackupState {
    data object Idle : BackupState
    data class Exporting(val message: String) : BackupState
    data object ExportReady : BackupState
    data class Importing(val message: String) : BackupState
    data class Success(val message: String) : BackupState
    data class Error(val message: String) : BackupState
}

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
    private var pendingExportFile: File? = null
    private val _arashiLaunch = container.arashiSyncSession.launch
    val arashiLaunch: SharedFlow<android.content.Intent> = _arashiLaunch

    init {
        viewModelScope.launch {
            container.settingsRepository.preferences.collect { prefs ->
                _uiState.update { current -> current.fromPrefs(prefs) }
            }
        }
        viewModelScope.launch {
            container.settingsRepository.lastArashiExport.collect { last ->
                _uiState.update { it.copy(lastArashiExport = last) }
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

    fun setBufferDuration(seconds: Int) {
        persistAudio {
            it.copy(
                bufferDurationSeconds = seconds.coerceIn(
                    AudioBufferConfig.MIN_SECONDS,
                    AudioBufferConfig.MAX_SECONDS,
                ),
            )
        }
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

    fun startExport(context: Context) {
        viewModelScope.launch {
            val tempFile = File(context.cacheDir, "hibiki_export_${System.currentTimeMillis()}.zip")
            pendingExportFile?.delete()
            pendingExportFile = null
            _uiState.update { it.copy(backupState = BackupState.Exporting("")) }
            try {
                container.backupRepository.exportBackup(tempFile).collect { state ->
                    _uiState.update {
                        it.copy(backupState = BackupState.Exporting(backupExportLabel(state)))
                    }
                    when (state) {
                        is BackupExportState.Completed -> {
                            pendingExportFile = tempFile
                            _uiState.update { it.copy(backupState = BackupState.ExportReady) }
                        }
                        is BackupExportState.Failed -> {
                            tempFile.delete()
                            pendingExportFile = null
                            _uiState.update {
                                it.copy(
                                    backupState = BackupState.Error(
                                        state.error.message ?: "Export fallito",
                                    ),
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (error: Exception) {
                tempFile.delete()
                pendingExportFile = null
                _uiState.update {
                    it.copy(backupState = BackupState.Error(error.message ?: "Export fallito"))
                }
            }
        }
    }

    fun saveExportToUri(context: Context, uri: Uri) {
        val tempFile = pendingExportFile
        if (tempFile == null || !tempFile.exists()) {
            _uiState.update {
                it.copy(backupState = BackupState.Error("Nessun export pronto da salvare"))
            }
            return
        }
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Impossibile scrivere il file di destinazione")
                tempFile.delete()
                pendingExportFile = null
                _uiState.update {
                    it.copy(backupState = BackupState.Success("Export completato"))
                }
            } catch (error: Exception) {
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                tempFile.delete()
                pendingExportFile = null
                _uiState.update {
                    it.copy(backupState = BackupState.Error(error.message ?: "Export fallito"))
                }
            }
        }
    }

    fun cancelPendingExport() {
        pendingExportFile?.delete()
        pendingExportFile = null
        _uiState.update { it.copy(backupState = BackupState.Idle) }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        val tempFile = File(context.cacheDir, "hibiki_import_${System.currentTimeMillis()}.zip")
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Impossibile leggere il file selezionato")
                importBackup(tempFile)
            } catch (error: Exception) {
                tempFile.delete()
                _uiState.update {
                    it.copy(backupState = BackupState.Error(error.message ?: "Import fallito"))
                }
            }
        }
    }

    fun importBackup(archiveFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(backupState = BackupState.Importing("")) }
            try {
                container.backupRepository.importBackup(archiveFile).collect { state ->
                    _uiState.update {
                        it.copy(backupState = BackupState.Importing(backupImportLabel(state)))
                    }
                    when (state) {
                        is BackupImportState.Completed -> {
                            archiveFile.delete()
                            _uiState.update {
                                it.copy(backupState = BackupState.Success("Backup importato"))
                            }
                        }
                        is BackupImportState.Failed -> {
                            archiveFile.delete()
                            _uiState.update {
                                it.copy(
                                    backupState = BackupState.Error(
                                        state.error.message ?: "Import fallito",
                                    ),
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (error: Exception) {
                archiveFile.delete()
                _uiState.update {
                    it.copy(backupState = BackupState.Error(error.message ?: "Import fallito"))
                }
            }
        }
    }

    fun clearBackupState() {
        _uiState.update { it.copy(backupState = BackupState.Idle) }
    }

    fun startArashiExport(context: Context) {
        viewModelScope.launch {
            container.arashiSyncSession.clearPending(context)
            _uiState.update { it.copy(arashiExportState = ArashiExportState.Preparing) }
            when (val immediate = container.arashiSyncSession.startPendingExport(context)) {
                null -> Unit
                else -> applyArashiOutcome(immediate)
            }
        }
    }

    fun onArashiLaunchFailed(context: Context, error: Throwable) {
        applyArashiOutcome(container.arashiSyncSession.onLaunchFailed(context, error))
    }

    fun onArashiActivityResult(
        context: Context,
        resultCode: Int,
        resultJson: String?,
        errorMessage: String?,
    ) {
        when (
            val result = container.arashiSyncSession.onActivityResult(
                context = context,
                resultCode = resultCode,
                resultJson = resultJson,
                errorMessage = errorMessage,
            )
        ) {
            is ArashiActivityResult.Done -> applyArashiOutcome(result.outcome)
            is ArashiActivityResult.SuccessPending -> {
                viewModelScope.launch {
                    val outcome = container.arashiSyncSession.finalizeSuccess(result.pending, result.result)
                    applyArashiOutcome(outcome)
                }
            }
        }
    }

    fun clearArashiExportState() {
        _uiState.update { it.copy(arashiExportState = ArashiExportState.Idle) }
    }

    private fun applyArashiOutcome(outcome: ArashiSyncOutcome) {
        when (outcome) {
            ArashiSyncOutcome.Cancelled -> {
                _uiState.update { it.copy(arashiExportState = ArashiExportState.Idle) }
            }
            is ArashiSyncOutcome.Error -> {
                _uiState.update { it.copy(arashiExportState = ArashiExportState.Error(outcome.message)) }
            }
            is ArashiSyncOutcome.Success -> {
                _uiState.update {
                    it.copy(arashiExportState = ArashiExportState.Success(outcome.summary))
                }
            }
        }
    }

    private fun backupExportLabel(state: BackupExportState): String = when (state) {
        BackupExportState.Preparing -> "Preparazione…"
        BackupExportState.CopyingDatabase -> "Copia database…"
        is BackupExportState.CopyingMedia -> "Media ${state.current}/${state.total}"
        BackupExportState.WritingManifest -> "Manifest…"
        BackupExportState.Compressing -> "Compressione…"
        BackupExportState.Validating -> "Validazione…"
        is BackupExportState.Completed -> "Completato"
        is BackupExportState.Failed -> "Errore"
    }

    private fun backupImportLabel(state: BackupImportState): String = when (state) {
        BackupImportState.Preparing -> "Preparazione…"
        BackupImportState.Extracting -> "Estrazione…"
        BackupImportState.ReadingManifest -> "Lettura manifest…"
        BackupImportState.ValidatingChecksums -> "Checksum…"
        BackupImportState.ValidatingDatabase -> "Validazione DB…"
        BackupImportState.BackingUpCurrentData -> "Backup sicurezza…"
        BackupImportState.ReplacingDatabase -> "Sostituzione DB…"
        BackupImportState.ReplacingMedia -> "Sostituzione media…"
        BackupImportState.ReopeningDatabase -> "Riapertura DB…"
        BackupImportState.FinalValidation -> "Validazione finale…"
        BackupImportState.Completed -> "Completato"
        BackupImportState.RollingBack -> "Rollback…"
        is BackupImportState.Failed -> "Errore"
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

    override fun onCleared() {
        pendingExportFile?.delete()
        super.onCleared()
    }

    companion object {
        private const val PERSIST_DEBOUNCE_MS = 400L
    }
}
