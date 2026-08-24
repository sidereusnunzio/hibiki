package com.hibiki.ui.home

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.BuildConfig
import com.hibiki.container
import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.ui.home.about.AboutUiState
import com.hibiki.ui.home.about.bytesToGb
import com.hibiki.ui.home.about.coveragePercent
import com.hibiki.ui.home.about.formatSessionId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container()
    private val sessionRandom = Random.Default
    private val sessionId = formatSessionId(sessionRandom)
    private val entropyPercent = 90f + sessionRandom.nextFloat() * 9.9f

    private val _uiState = MutableStateFlow(
        AboutUiState(
            buildId = BuildConfig.APP_UPDATE_ID,
            analysisPromptVersion = DefaultPrompts.ANALYSIS_PROMPT_VERSION,
            sessionId = sessionId,
            entropyPercent = entropyPercent,
            currentBuildDate = LocalDate.now().format(BUILD_DATE_FORMAT),
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            deviceModel = Build.MODEL.orEmpty().ifBlank { "UNKNOWN" },
            localeTag = Locale.getDefault().toLanguageTag(),
        ),
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        loadDiagnostics()
    }

    private fun loadDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val db = container.databaseProvider.getDatabase()
                val phrases = db.phraseDao().getAll()
                val samples = db.audioSampleDao().getAll()
                val contexts = db.contextDao().getAll()
                val subjects = db.subjectDao().getAll()
                val prefs = container.settingsRepository.preferences.first()

                val audioPresent = samples.count { !it.audioPath.isNullOrBlank() }
                val audioMissing = samples.size - audioPresent
                val fingerprintPresent = samples.count { it.audioFingerprint != null }
                val fingerprintMissing = samples.size - fingerprintPresent

                val memory = readMemory(getApplication())
                val storage = readStorage()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        buildId = BuildConfig.APP_UPDATE_ID,
                        analysisPromptVersion = DefaultPrompts.ANALYSIS_PROMPT_VERSION,
                        phraseCount = phrases.size,
                        audioSampleCount = samples.size,
                        contextCount = contexts.size,
                        subjectCount = subjects.size,
                        databaseIntegrityLabel = "OK",
                        databaseIntegrityOk = true,
                        fingerprintIndexOnline = fingerprintPresent > 0 || samples.isEmpty(),
                        audioCoveragePercent = coveragePercent(audioPresent, audioMissing),
                        fingerprintCoveragePercent = coveragePercent(
                            fingerprintPresent,
                            fingerprintMissing,
                        ),
                        ramUsedGb = memory.usedGb,
                        ramTotalGb = memory.totalGb,
                        storageUsedGb = storage.usedGb,
                        storageTotalGb = storage.totalGb,
                        transcriptionModelLabel = prefs.api.transcriptionModel
                            .uppercase(Locale.ROOT),
                        analysisModelLabel = prefs.api.languageAnalysisModel
                            .uppercase(Locale.ROOT),
                        aiAvailable = prefs.api.hasApiKey,
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        databaseIntegrityLabel = "WARN",
                        databaseIntegrityOk = false,
                        fingerprintIndexOnline = false,
                    )
                }
            }
        }
    }

    private fun readMemory(context: Context): MemorySnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val total = info.totalMem.coerceAtLeast(1L)
        val used = (total - info.availMem).coerceAtLeast(0L)
        return MemorySnapshot(
            usedGb = bytesToGb(used),
            totalGb = bytesToGb(total),
        )
    }

    private fun readStorage(): MemorySnapshot {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.absolutePath)
        val total = stat.totalBytes.coerceAtLeast(1L)
        val used = (total - stat.availableBytes).coerceAtLeast(0L)
        return MemorySnapshot(
            usedGb = bytesToGb(used),
            totalGb = bytesToGb(total),
        )
    }

    private data class MemorySnapshot(
        val usedGb: Float,
        val totalGb: Float,
    )

    private companion object {
        val BUILD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
