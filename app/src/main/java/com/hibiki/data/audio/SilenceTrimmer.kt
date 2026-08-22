package com.hibiki.data.audio

object SilenceTrimmer {
    fun trim(clip: PcmClip, rmsThreshold: Double = 280.0, padMs: Int = 40): PcmClip {
        if (clip.samples.isEmpty()) return clip
        val frame = (clip.sampleRate / 100).coerceAtLeast(80)
        val first = firstLoudFrame(clip.samples, frame, rmsThreshold) ?: return clip.copy(samples = ShortArray(0))
        val last = lastLoudFrame(clip.samples, frame, rmsThreshold) ?: return clip.copy(samples = ShortArray(0))
        val pad = clip.sampleRate * padMs / 1000
        val start = (first - pad).coerceAtLeast(0)
        val end = (last + pad).coerceAtMost(clip.samples.size)
        if (end <= start) return clip.copy(samples = ShortArray(0))
        return clip.copy(samples = clip.samples.copyOfRange(start, end))
    }

    private fun firstLoudFrame(samples: ShortArray, frame: Int, threshold: Double): Int? {
        var index = 0
        while (index < samples.size) {
            val end = (index + frame).coerceAtMost(samples.size)
            if (rms(samples, index, end) >= threshold) return index
            index = end
        }
        return null
    }

    private fun lastLoudFrame(samples: ShortArray, frame: Int, threshold: Double): Int? {
        var index = samples.size
        while (index > 0) {
            val start = (index - frame).coerceAtLeast(0)
            if (rms(samples, start, index) >= threshold) return index
            index = start
        }
        return null
    }

    private fun rms(samples: ShortArray, start: Int, end: Int): Double {
        var acc = 0.0
        val count = (end - start).coerceAtLeast(1)
        for (i in start until end) {
            val value = samples[i].toDouble()
            acc += value * value
        }
        return kotlin.math.sqrt(acc / count)
    }
}
