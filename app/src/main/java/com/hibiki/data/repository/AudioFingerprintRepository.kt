package com.hibiki.data.repository

import com.hibiki.data.audio.AudioFingerprinter
import com.hibiki.data.audio.PcmClip
import com.hibiki.domain.model.AudioSample

data class SampleFingerprintMatch(
    val sample: AudioSample,
    val similarity: Float,
)

class AudioFingerprintRepository {
    fun fingerprint(clip: PcmClip): ByteArray = AudioFingerprinter.compute(clip)

    fun findBestMatch(
        fingerprint: ByteArray,
        candidates: List<AudioSample>,
        threshold: Float,
        durationMs: Long,
    ): SampleFingerprintMatch? {
        if (fingerprint.isEmpty()) return null
        var best: SampleFingerprintMatch? = null
        for (sample in candidates) {
            val stored = sample.audioFingerprint ?: continue
            if (!AudioFingerprinter.durationCompatible(durationMs, sample.durationMs)) continue
            val similarity = AudioFingerprinter.similarity(fingerprint, stored)
            if (similarity >= threshold && (best == null || similarity > best.similarity)) {
                best = SampleFingerprintMatch(sample, similarity)
            }
        }
        return best
    }
}
