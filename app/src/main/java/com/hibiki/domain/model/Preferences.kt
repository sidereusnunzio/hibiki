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
    val bufferEnabled: Boolean = false,
    val collapsed: Boolean = false,
    val selectedContext: StudyContext? = null,
    val selectedSubject: Subject? = null,
    val contexts: List<StudyContext> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val result: CaptureResult? = null,
    val phrasePanelVisible: Boolean = false,
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
    val bufferDurationSeconds: Int = AudioBufferConfig.DEFAULT_SECONDS,
)

enum class AudioFormat(val fileExtension: String, val mimeType: String) {
    AAC("m4a", "audio/mp4"),
}

/**
 * Parametri di matching audio.
 * La durata totale non è un criterio di decisione (record/stop manuale).
 * Si confronta uno spezzone intorno a metà clip cercato nei candidati.
 */
object AudioMatchConfig {
    const val DEFAULT_SIMILARITY_THRESHOLD = 0.92f
    /** Stage 1: retrieval opzionale — allarga il pool, non decide il match. */
    const val RETRIEVAL_SIMILARITY_THRESHOLD = 0.70f
    const val RETRIEVAL_TOP_CANDIDATES = 15
    /**
     * Spezzone preso dalla nuova registrazione: centro poco dopo metà,
     * durata tipica ~500 ms (clamp su clip cortissime).
     */
    const val PROBE_CENTER_RATIO = 0.55f
    const val PROBE_DURATION_MS = 500L
    const val PROBE_MIN_MS = 250L
    /** Soglia relativa al picco per individuare la regione sonora (esclude silenzi di start/stop). */
    const val ACTIVE_ENERGY_RATIO = 0.15f
    /** Passo di ricerca temporale (ms): non severo sul timing. */
    const val SEGMENT_SEARCH_HOP_MS = 15L
    /** Stage 2: lo spezzone coincide abbastanza con un tratto dell'archivio. */
    const val STRONG_SEGMENT_SCORE = 0.88f
    const val MAX_PROTOTYPES_PER_PHRASE = 3
    const val DURATION_TOLERANCE_MIN_MS = 300L
    const val DURATION_TOLERANCE_RATIO = 0.20
}

object AudioBufferConfig {
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 5
    const val DEFAULT_SECONDS = 2
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
    val archiveFilters: ArchiveFilters = ArchiveFilters(),
    val overlayX: Int = 24,
    val overlayY: Int = 180,
    val overlayCollapsed: Boolean = false,
    val overlayDisplay: OverlayDisplayPrefs = OverlayDisplayPrefs(),
    val audio: AudioSettings = AudioSettings(),
    val api: ApiSettings = ApiSettings(),
)
