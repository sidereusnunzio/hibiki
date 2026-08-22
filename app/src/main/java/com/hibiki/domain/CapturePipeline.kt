package com.hibiki.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hibiki.data.audio.AacEncoder
import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.audio.PcmClip
import com.hibiki.data.repository.AudioFingerprintRepository
import com.hibiki.data.repository.LanguageAnalysisRepository
import com.hibiki.data.repository.PhraseRepository
import com.hibiki.data.repository.SettingsRepository
import com.hibiki.data.repository.TranscriptionRepository
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import com.hibiki.domain.model.AudioSample
import com.hibiki.domain.model.CaptureResult
import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.LinguisticAnalysis
import com.hibiki.domain.model.OverlayStage
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.PhraseSource
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.flow.first
import java.util.UUID

class CapturePipeline(
    private val appContext: Context,
    private val fingerprintRepository: AudioFingerprintRepository,
    private val phraseRepository: PhraseRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val languageAnalysisRepository: LanguageAnalysisRepository,
    private val settingsRepository: SettingsRepository,
    private val audioFileStore: AudioFileStore,
) {
    suspend fun process(
        clip: PcmClip,
        context: StudyContext,
        subject: Subject?,
        onStage: suspend (OverlayStage) -> Unit,
    ): CaptureResult {
        onStage(OverlayStage.PROCESSING_AUDIO)
        // PCM già trimmato in capture: fingerprint + durata prima di qualsiasi AAC.
        val fingerprint = fingerprintRepository.fingerprint(clip)
        val prefs = settingsRepository.preferences.first()

        onStage(OverlayStage.SEARCHING_LOCAL_ARCHIVE)
        val sampleMatch = fingerprintRepository.findBestMatch(
            fingerprint = fingerprint,
            candidates = phraseRepository.samplesForFingerprint(),
            threshold = prefs.audio.fingerprintThreshold,
            durationMs = clip.durationMs,
        )
        if (sampleMatch != null) {
            val existing = phraseRepository.findAnalysis(
                audioSampleId = sampleMatch.sample.id,
                contextId = context.id,
                subjectId = subject?.id,
            )
            if (existing != null) {
                return CaptureResult(
                    phrase = existing,
                    origin = PhraseSource.LOCAL_MATCH,
                    similarity = sampleMatch.similarity,
                )
            }
            ensureNetwork()
            onStage(OverlayStage.ANALYZING)
            val analysis = languageAnalysisRepository.analyze(
                sampleMatch.sample.japaneseRaw,
                context,
                subject,
            )
            val phrase = analysis.toPhrase(
                sample = sampleMatch.sample,
                contextId = context.id,
                subjectId = subject?.id,
                analysisModel = prefs.api.languageAnalysisModel,
            )
            phraseRepository.insertPhrase(phrase)
            return CaptureResult(phrase = phrase, origin = PhraseSource.API, similarity = sampleMatch.similarity)
        }

        ensureNetwork()
        val temp = audioFileStore.createTempFile("m4a")
        var persistedPath: String? = null
        try {
            runCatching { AacEncoder.encodeToM4a(clip, temp) }
                .getOrElse { throw AppException(AppError.FileSaveFailed, it) }

            onStage(OverlayStage.TRANSCRIBING)
            val transcription = transcriptionRepository.transcribe(
                audioFile = temp,
                prompt = TranscriptionPromptBuilder.build(context, subject),
                language = context.expectedLanguage.ifBlank { "ja" },
            )
            if (prefs.audio.saveAudio) {
                persistedPath = audioFileStore.persist(temp)
            }
            val sample = AudioSample(
                id = UUID.randomUUID().toString(),
                audioPath = persistedPath,
                audioFingerprint = fingerprint,
                durationMs = clip.durationMs,
                japaneseRaw = transcription.text,
                confidence = transcription.confidence,
                transcriptionModel = prefs.api.transcriptionModel,
                transcriptionPromptVersion = DefaultPrompts.TRANSCRIPTION_PROMPT_VERSION,
                createdAt = System.currentTimeMillis(),
            )
            phraseRepository.insertSample(sample)

            onStage(OverlayStage.ANALYZING)
            val analysis = languageAnalysisRepository.analyze(transcription.text, context, subject)
            val phrase = analysis.toPhrase(
                sample = sample,
                contextId = context.id,
                subjectId = subject?.id,
                analysisModel = prefs.api.languageAnalysisModel,
            )
            phraseRepository.insertPhrase(phrase)
            return CaptureResult(phrase = phrase, origin = PhraseSource.API)
        } finally {
            audioFileStore.deleteQuietly(temp)
        }
    }

    suspend fun reanalyze(
        japanese: String,
        context: StudyContext? = null,
        subject: Subject? = null,
    ): LinguisticAnalysis {
        ensureNetwork()
        return languageAnalysisRepository.analyze(japanese, context, subject)
    }

    private fun LinguisticAnalysis.toPhrase(
        sample: AudioSample,
        contextId: String,
        subjectId: String?,
        analysisModel: String,
    ) = Phrase(
        id = UUID.randomUUID().toString(),
        audioSampleId = sample.id,
        contextId = contextId,
        subjectId = subjectId,
        audioPath = sample.audioPath,
        audioFingerprint = sample.audioFingerprint,
        durationMs = sample.durationMs,
        japaneseRaw = sample.japaneseRaw,
        japaneseCorrected = null,
        kana = kana,
        romaji = romaji,
        literalTranslation = literalTranslation,
        naturalTranslation = naturalTranslation,
        confidence = sample.confidence,
        verified = false,
        source = PhraseSource.API,
        createdAt = System.currentTimeMillis(),
        transcriptionModel = sample.transcriptionModel,
        analysisModel = analysisModel,
        transcriptionPromptVersion = sample.transcriptionPromptVersion,
        analysisPromptVersion = DefaultPrompts.ANALYSIS_PROMPT_VERSION,
    )

    private fun ensureNetwork() {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: throw AppException(AppError.NoNetwork)
        val caps = manager.getNetworkCapabilities(network) ?: throw AppException(AppError.NoNetwork)
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) throw AppException(AppError.NoNetwork)
    }
}
