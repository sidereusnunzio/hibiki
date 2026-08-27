package com.hibiki.domain

import com.hibiki.data.audio.AudioAligner
import com.hibiki.data.audio.AudioDecoder
import com.hibiki.data.audio.PcmClip
import com.hibiki.data.audio.PcmPreviewCodec
import com.hibiki.data.audio.RecordedClip
import com.hibiki.data.repository.AudioFingerprintRepository
import com.hibiki.data.repository.PhraseMatchTarget
import com.hibiki.data.repository.PhraseRepository
import com.hibiki.domain.model.AudioMatchConfig
import com.hibiki.domain.model.Phrase
import java.io.File

data class LocalPhraseMatchResult(
    val phrase: Phrase,
    val fingerprintScore: Float,
    val alignmentScore: Float,
    val matchedCoverage: Float,
)

class LocalPhraseMatcher(
    private val fingerprintRepository: AudioFingerprintRepository,
    private val phraseRepository: PhraseRepository,
) {
    suspend fun match(
        recorded: RecordedClip,
        contextId: String,
        subjectId: String?,
    ): LocalPhraseMatchResult? {
        val newClips = listOf(recorded.trimmed, recorded.raw)
        val targets = phraseRepository.getMatchTargets(
            contextId = contextId,
            subjectId = subjectId,
            clipDurationMs = recorded.trimmed.durationMs,
        )
        if (targets.isEmpty()) return null

        val fingerprints = newClips
            .map { fingerprintRepository.fingerprint(it) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.toList() }
        val fingerprintScoreByPrototype = if (fingerprints.isEmpty()) {
            emptyMap()
        } else {
            fingerprintRepository.retrieveCandidates(
                queryFingerprints = fingerprints,
                targets = targets,
            ).associate { it.target.prototypeId to it.fingerprintScore }
        }
        // Pool già filtrato per contesto, personaggio e durata (±5 s).
        // Il fingerprint serve solo a ordinare i confronti (candidati più plausibili prima).
        val pool = targets
            .sortedByDescending { fingerprintScoreByPrototype[it.prototypeId] ?: -1f }
            .map { it to (fingerprintScoreByPrototype[it.prototypeId] ?: 0f) }

        var best: LocalPhraseMatchResult? = null
        for ((target, fingerprintScore) in pool) {
            val referenceClip = loadReferenceClip(target) ?: continue
            val alignment = AudioAligner.bestMidSegmentMatch(newClips, referenceClip) ?: continue
            if (alignment.alignmentScore < AudioMatchConfig.STRONG_SEGMENT_SCORE) continue
            if (best == null || alignment.alignmentScore > best.alignmentScore) {
                best = LocalPhraseMatchResult(
                    phrase = target.phrase,
                    fingerprintScore = fingerprintScore,
                    alignmentScore = alignment.alignmentScore,
                    matchedCoverage = alignment.matchedCoverage,
                )
            }
        }
        return best
    }

    private fun loadReferenceClip(target: PhraseMatchTarget): PcmClip? {
        PcmPreviewCodec.decode(target.pcmPreview)?.let { samples ->
            if (samples.isNotEmpty()) return PcmClip(samples, sampleRate = 16_000)
        }
        val path = target.audioPath ?: return null
        return AudioDecoder.decodeToPcm(File(path))
    }
}
