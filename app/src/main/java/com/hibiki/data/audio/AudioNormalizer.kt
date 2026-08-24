package com.hibiki.data.audio

import kotlin.math.abs
import kotlin.math.sqrt

object AudioNormalizer {
    private const val TARGET_RATE = 16_000

    fun toMatchPcm(clip: PcmClip): PcmClip {
        val mono = downsampleToMono(clip, TARGET_RATE)
        if (mono.isEmpty()) return PcmClip(ShortArray(0), TARGET_RATE)
        val normalized = normalizeAmplitude(removeDc(mono))
        return PcmClip(normalized, TARGET_RATE)
    }

    fun toPreviewPcm(clip: PcmClip): ShortArray = toMatchPcm(clip).samples

    private fun downsampleToMono(clip: PcmClip, targetRate: Int): DoubleArray {
        val step = clip.sampleRate.toDouble() / targetRate
        val channels = clip.channelCount.coerceAtLeast(1)
        val frameCount = clip.samples.size / channels
        if (frameCount <= 0) return DoubleArray(0)
        val outSize = (frameCount / step).toInt().coerceAtLeast(0)
        if (outSize == 0) return DoubleArray(0)
        val out = DoubleArray(outSize)
        var src = 0.0
        for (i in 0 until outSize) {
            val index = (src.toInt() * channels).coerceAtMost(clip.samples.lastIndex)
            out[i] = clip.samples[index] / 32768.0
            src += step
        }
        return out
    }

    private fun removeDc(samples: DoubleArray): DoubleArray {
        if (samples.isEmpty()) return samples
        val mean = samples.average()
        return DoubleArray(samples.size) { index -> samples[index] - mean }
    }

    private fun normalizeAmplitude(samples: DoubleArray): ShortArray {
        if (samples.isEmpty()) return ShortArray(0)
        var peak = 0.0
        for (sample in samples) {
            peak = maxOf(peak, abs(sample))
        }
        val scale = if (peak < 1e-6) 1.0 else 0.95 / peak
        return ShortArray(samples.size) { index ->
            (samples[index] * scale * 32767.0).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    fun normalizedCrossCorrelation(reference: ShortArray, query: ShortArray, offset: Int): Double {
        if (reference.isEmpty() || query.isEmpty() || offset < 0 || offset + reference.size > query.size) {
            return 0.0
        }
        var sumAB = 0.0
        var sumA2 = 0.0
        var sumB2 = 0.0
        for (index in reference.indices) {
            val a = reference[index].toDouble()
            val b = query[offset + index].toDouble()
            sumAB += a * b
            sumA2 += a * a
            sumB2 += b * b
        }
        val denom = sqrt(sumA2 * sumB2)
        return if (denom < 1e-9) 0.0 else sumAB / denom
    }
}
