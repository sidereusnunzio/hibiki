package com.hibiki.data.repository

import com.hibiki.data.audio.AudioFingerprinter
import com.hibiki.data.audio.PcmClip
import com.hibiki.domain.model.AudioMatchConfig

data class FingerprintRetrievalCandidate(
    val target: PhraseMatchTarget,
    val fingerprintScore: Float,
)

class AudioFingerprintRepository {
    fun fingerprint(clip: PcmClip): ByteArray = AudioFingerprinter.compute(clip)

    fun retrieveCandidates(
        queryFingerprints: List<ByteArray>,
        targets: List<PhraseMatchTarget>,
        threshold: Float = AudioMatchConfig.RETRIEVAL_SIMILARITY_THRESHOLD,
        topN: Int = AudioMatchConfig.RETRIEVAL_TOP_CANDIDATES,
    ): List<FingerprintRetrievalCandidate> {
        val queries = queryFingerprints.filter { it.isNotEmpty() }
        if (queries.isEmpty() || targets.isEmpty()) return emptyList()
        return targets
            .mapNotNull { target ->
                val stored = target.fingerprint ?: return@mapNotNull null
                if (stored.isEmpty()) return@mapNotNull null
                val score = queries.maxOf { AudioFingerprinter.similarity(it, stored) }
                if (score < threshold) return@mapNotNull null
                FingerprintRetrievalCandidate(target, score)
            }
            .sortedByDescending { it.fingerprintScore }
            .take(topN)
    }
}
