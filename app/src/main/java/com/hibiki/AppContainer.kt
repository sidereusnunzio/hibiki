package com.hibiki

import android.content.Context
import com.hibiki.data.api.openai.OpenAiProvider
import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.audio.PhraseAudioPlayer
import com.hibiki.data.arashi.ArashiExportService
import com.hibiki.data.arashi.ArashiSyncSession
import com.hibiki.data.backup.BackupExportService
import com.hibiki.data.backup.BackupImportService
import com.hibiki.data.crypto.SecureApiKeyStore
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.media.ImageFileStore
import com.hibiki.data.media.MediaDirectories
import com.hibiki.data.media.UmamusumePortraitSeeder
import com.hibiki.data.repository.AudioCaptureRepository
import com.hibiki.data.repository.AudioFingerprintRepository
import com.hibiki.data.repository.BackupRepository
import com.hibiki.data.repository.ContextRepository
import com.hibiki.data.repository.LanguageAnalysisRepository
import com.hibiki.data.repository.PhraseRepository
import com.hibiki.data.repository.SettingsRepository
import com.hibiki.data.repository.TranscriptionRepository
import com.hibiki.domain.CapturePipeline
import com.hibiki.domain.LocalPhraseMatcher
import com.hibiki.overlay.OverlayController
import com.hibiki.ui.archive.ArchiveBrowseSession

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val databaseProvider = HibikiDatabaseProvider(appContext)
    val mediaDirectories = MediaDirectories(appContext)
    val audioFileStore = AudioFileStore(appContext)
    val imageFileStore = ImageFileStore(mediaDirectories.imagesDir)
    val umamusumePortraitSeeder = UmamusumePortraitSeeder(
        context = appContext,
        databaseProvider = databaseProvider,
        imageFileStore = imageFileStore,
    )
    val apiKeyStore = SecureApiKeyStore(appContext)
    val settingsRepository = SettingsRepository(appContext, apiKeyStore)
    val contextRepository = ContextRepository(databaseProvider, imageFileStore)
    val phraseRepository = PhraseRepository(databaseProvider, audioFileStore)
    val audioCaptureRepository = AudioCaptureRepository()
    val audioFingerprintRepository = AudioFingerprintRepository()
    val localPhraseMatcher = LocalPhraseMatcher(
        fingerprintRepository = audioFingerprintRepository,
        phraseRepository = phraseRepository,
    )
    val phraseAudioPlayer = PhraseAudioPlayer(appContext)
    val backupRepository = BackupRepository(
        exportService = BackupExportService(appContext, databaseProvider, mediaDirectories),
        importService = BackupImportService(
            appContext,
            databaseProvider,
            mediaDirectories,
            umamusumePortraitSeeder,
        ),
    )
    val arashiExportService = ArashiExportService(
        phraseRepository = phraseRepository,
        databaseProvider = databaseProvider,
    )
    val arashiSyncSession = ArashiSyncSession(
        arashiExportService = arashiExportService,
        phraseRepository = phraseRepository,
        settingsRepository = settingsRepository,
    )
    val archiveBrowseSession = ArchiveBrowseSession()

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
        localPhraseMatcher = localPhraseMatcher,
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
