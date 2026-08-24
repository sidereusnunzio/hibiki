package com.hibiki.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hibiki.data.audio.AacEncoder
import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.audio.AudioNormalizer
import com.hibiki.data.audio.PcmPreviewCodec
import com.hibiki.data.audio.RecordedClip
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
    private val localPhraseMatcher: LocalPhraseMatcher,
    private val phraseRepository: PhraseRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val languageAnalysisRepository: LanguageAnalysisRepository,
    private val settingsRepository: SettingsRepository,
    private val audioFileStore: AudioFileStore,
) {
    suspend fun process(
        recorded: RecordedClip,
        context: StudyContext,
        subject: Subject?,
        onStage: suspend (OverlayStage) -> Unit,
    ): CaptureResult {
        val clip = recorded.trimmed
        onStage(OverlayStage.PROCESSING_AUDIO)
        val prefs = settingsRepository.preferences.first()

        onStage(OverlayStage.SEARCHING_LOCAL_ARCHIVE)
        val localMatch = localPhraseMatcher.match(
            recorded = recorded,
            contextId = context.id,
            subjectId = subject?.id,
        )
        if (localMatch != null) {
            return CaptureResult(
                phrase = localMatch.phrase,
                origin = PhraseSource.LOCAL_MATCH,
                similarity = localMatch.alignmentScore,
            )
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

            val duplicate = phraseRepository.findDuplicateAnalysis(
                contextId = context.id,
                subjectId = subject?.id,
                japaneseRaw = transcription.text,
            )
            if (duplicate != null) {
                phraseRepository.addPrototypeFromRecording(
                    phraseId = duplicate.id,
                    fingerprint = primaryFingerprint(recorded),
                    durationMs = clip.durationMs,
                    recorded = recorded,
                )
                if (phraseRepository.hasCompleteAnalysis(duplicate)) {
                    return CaptureResult(
                        phrase = duplicate,
                        origin = PhraseSource.TEXT_MATCH_AFTER_TRANSCRIPTION,
                    )
                }
                onStage(OverlayStage.ANALYZING)
                val analysis = languageAnalysisRepository.analyze(transcription.text, context, subject)
                phraseRepository.updateLinguisticFields(duplicate.id, analysis, japaneseCorrected = null)
                return CaptureResult(
                    phrase = duplicate.copy(
                        kana = analysis.kana,
                        romaji = analysis.romaji,
                        literalTranslation = analysis.literalTranslation,
                        naturalTranslation = analysis.naturalTranslation,
                        updatedAt = System.currentTimeMillis(),
                    ),
                    origin = PhraseSource.TEXT_MATCH_AFTER_TRANSCRIPTION,
                )
            }

            if (prefs.audio.saveAudio) {
                persistedPath = audioFileStore.persist(temp)
            }
            val primaryFingerprint = primaryFingerprint(recorded)
            val sample = AudioSample(
                id = UUID.randomUUID().toString(),
                audioPath = persistedPath,
                audioFingerprint = primaryFingerprint,
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
            phraseRepository.findDuplicateAnalysis(
                contextId = context.id,
                subjectId = subject?.id,
                japaneseRaw = transcription.text,
            )?.let { raceDuplicate ->
                phraseRepository.addPrototypeFromRecording(
                    phraseId = raceDuplicate.id,
                    fingerprint = primaryFingerprint,
                    durationMs = clip.durationMs,
                    recorded = recorded,
                )
                return CaptureResult(
                    phrase = raceDuplicate,
                    origin = PhraseSource.TEXT_MATCH_AFTER_TRANSCRIPTION,
                )
            }
            val phrase = analysis.toPhrase(
                sample = sample,
                contextId = context.id,
                subjectId = subject?.id,
                analysisModel = prefs.api.languageAnalysisModel,
            )
            val pcmPreview = PcmPreviewCodec.encode(AudioNormalizer.toPreviewPcm(recorded.trimmed))
            phraseRepository.insertPhrase(phrase, primaryFingerprint, pcmPreview)
            return CaptureResult(phrase = phrase, origin = PhraseSource.API)
        } finally {
            audioFileStore.deleteQuietly(temp)
        }
    }

    private fun primaryFingerprint(recorded: RecordedClip): ByteArray =
        listOf(
            fingerprintRepository.fingerprint(recorded.trimmed),
            fingerprintRepository.fingerprint(recorded.raw),
        )
            .filter { it.isNotEmpty() }
            .maxByOrNull { it.size }
            ?: ByteArray(0)

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
        updatedAt = System.currentTimeMillis(),
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
