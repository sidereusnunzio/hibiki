package com.hibiki.domain.model

enum class OverlayStage {
    IDLE,
    LISTENING,
    PROCESSING_AUDIO,
    SEARCHING_LOCAL_ARCHIVE,
    TRANSCRIBING,
    ANALYZING,
    RESULT,
    ERROR,
}

data class OverlayUiState(
    val stage: OverlayStage = OverlayStage.IDLE,
    val collapsed: Boolean = false,
    val selectedContext: StudyContext? = null,
    val selectedSubject: Subject? = null,
    val contexts: List<StudyContext> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val result: CaptureResult? = null,
    val errorMessage: String? = null,
    val remainingSeconds: Int? = null,
)

data class OverlayDisplayPrefs(
    val showJapanese: Boolean = true,
    val showKana: Boolean = true,
    val showRomaji: Boolean = false,
    val showLiteral: Boolean = false,
    val showNatural: Boolean = true,
)

data class AudioSettings(
    val maxDurationSeconds: Int = 10,
    val saveAudio: Boolean = true,
    val audioFormat: AudioFormat = AudioFormat.AAC,
    val fingerprintThreshold: Float = AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD,
    val trimSilence: Boolean = true,
)

enum class AudioFormat(val fileExtension: String, val mimeType: String) {
    AAC("m4a", "audio/mp4"),
}

/**
 * Parametri di matching audio.
 * Soglia e tolleranza durata vanno calibrate su registrazioni reali: non modificarle senza dati.
 */
object AudioMatchConfig {
    const val DEFAULT_SIMILARITY_THRESHOLD = 0.92f
    const val DURATION_TOLERANCE_MIN_MS = 300L
    const val DURATION_TOLERANCE_RATIO = 0.15
}

data class ApiSettings(
    val providerId: String = ApiProviderId.OPENAI,
    val transcriptionModel: String = "gpt-transcribe",
    val languageAnalysisModel: String = "gpt-4o-mini",
    val hasApiKey: Boolean = false,
)

object ApiProviderId {
    const val OPENAI = "openai"
}

data class AppPreferences(
    val lastContextId: String = BuiltInIds.GENERAL,
    val lastSubjectIds: Map<String, String> = emptyMap(),
    val overlayX: Int = 24,
    val overlayY: Int = 180,
    val overlayCollapsed: Boolean = false,
    val overlayDisplay: OverlayDisplayPrefs = OverlayDisplayPrefs(),
    val audio: AudioSettings = AudioSettings(),
    val api: ApiSettings = ApiSettings(),
)
