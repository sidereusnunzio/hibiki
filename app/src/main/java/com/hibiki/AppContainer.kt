package com.hibiki

import android.content.Context
import com.hibiki.data.api.openai.OpenAiProvider
import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.audio.PhraseAudioPlayer
import com.hibiki.data.crypto.SecureApiKeyStore
import com.hibiki.data.local.HibikiDatabase
import com.hibiki.data.repository.AudioCaptureRepository
import com.hibiki.data.repository.AudioFingerprintRepository
import com.hibiki.data.repository.ContextRepository
import com.hibiki.data.repository.LanguageAnalysisRepository
import com.hibiki.data.repository.PhraseRepository
import com.hibiki.data.repository.SettingsRepository
import com.hibiki.data.repository.TranscriptionRepository
import com.hibiki.domain.CapturePipeline
import com.hibiki.overlay.OverlayController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: HibikiDatabase = HibikiDatabase.create(appContext)
    val audioFileStore = AudioFileStore(appContext)
    val apiKeyStore = SecureApiKeyStore(appContext)
    val settingsRepository = SettingsRepository(appContext, apiKeyStore)
    val contextRepository = ContextRepository(database)
    val phraseRepository = PhraseRepository(database, audioFileStore)
    val audioCaptureRepository = AudioCaptureRepository()
    val audioFingerprintRepository = AudioFingerprintRepository()
    val phraseAudioPlayer = PhraseAudioPlayer(appContext)

    @Volatile
    var latestModels: Pair<String, String> = "gpt-transcribe" to "gpt-4o-mini"

    private val openAiProvider = OpenAiProvider(
        apiKeyProvider = { settingsRepository.getApiKey() },
        transcriptionModelProvider = { latestModels.first },
        analysisModelProvider = { latestModels.second },
    )

    val transcriptionRepository = TranscriptionRepository(openAiProvider)
    val languageAnalysisRepository = LanguageAnalysisRepository(openAiProvider)

    val capturePipeline = CapturePipeline(
        appContext = appContext,
        fingerprintRepository = audioFingerprintRepository,
        phraseRepository = phraseRepository,
        transcriptionRepository = transcriptionRepository,
        languageAnalysisRepository = languageAnalysisRepository,
        settingsRepository = settingsRepository,
        audioFileStore = audioFileStore,
    )

    val overlayController = OverlayController(
        contextRepository = contextRepository,
        settingsRepository = settingsRepository,
        audioCaptureRepository = audioCaptureRepository,
        capturePipeline = capturePipeline,
    )
}

fun Context.container(): AppContainer = (applicationContext as HibikiApplication).container
