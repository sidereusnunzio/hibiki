package com.hibiki.ui.home.about

data class AboutUiState(
    val isLoading: Boolean = true,
    val buildId: String = "",
    val analysisPromptVersion: Int = 0,
    val phraseCount: Int = 0,
    val audioSampleCount: Int = 0,
    val contextCount: Int = 0,
    val subjectCount: Int = 0,
    val databaseIntegrityLabel: String = "—",
    val databaseIntegrityOk: Boolean = true,
    val fingerprintIndexOnline: Boolean = true,
    val audioCoveragePercent: Int = 100,
    val fingerprintCoveragePercent: Int = 100,
    val androidVersion: String = "",
    val deviceModel: String = "",
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val localeTag: String = "",
    val transcriptionModelLabel: String = "—",
    val analysisModelLabel: String = "—",
    val aiAvailable: Boolean = false,
    val sessionId: String = "",
    val entropyPercent: Float = 0f,
    val bootDate: String = BOOT_DATE,
    val firstPhraseDate: String = FIRST_PHRASE_DATE,
    val currentBuildDate: String = "",
)

const val BOOT_DATE = "2026.08.22"
const val FIRST_PHRASE_DATE = "2026.08.22"

internal fun coveragePercent(present: Int, missing: Int): Int {
    val total = present + missing
    if (total <= 0) return 100
    return ((present * 100f) / total).toInt().coerceIn(0, 100)
}

internal fun formatSessionId(random: kotlin.random.Random = kotlin.random.Random.Default): String =
    List(5) { "%02X".format(random.nextInt(0, 256)) }.joinToString("-")

internal fun bytesToGb(bytes: Long): Float =
    (bytes / (1024.0 * 1024.0 * 1024.0)).toFloat()
