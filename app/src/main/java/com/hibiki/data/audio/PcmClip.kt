package com.hibiki.data.audio

import com.hibiki.domain.model.AudioMatchConfig

data class PcmClip(
    val samples: ShortArray,
    val sampleRate: Int,
    val channelCount: Int = 1,
) {
    val durationMs: Long
        get() = if (sampleRate <= 0 || samples.isEmpty()) {
            0L
        } else {
            samples.size * 1000L / (sampleRate * channelCount)
        }

    val isEmpty: Boolean get() = samples.isEmpty() || durationMs < AudioMatchConfig.MIN_CAPTURE_DURATION_MS
}
