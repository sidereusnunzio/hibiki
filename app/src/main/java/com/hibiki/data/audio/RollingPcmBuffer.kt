package com.hibiki.data.audio

import com.hibiki.domain.model.AudioBufferConfig

/**
 * Circular PCM buffer that retains the most recent [durationMs] of mono 16-bit samples.
 */
class RollingPcmBuffer(
    sampleRate: Int,
    val durationMs: Int = DEFAULT_DURATION_MS,
) {
    private val capacity = (sampleRate.toLong() * durationMs / 1000L).toInt().coerceAtLeast(1)
    private val data = ShortArray(capacity)
    private var writePos = 0
    private var size = 0
    private val lock = Any()

    fun write(src: ShortArray, offset: Int, length: Int) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= src.size)
        synchronized(lock) {
            var remaining = length
            var from = offset
            while (remaining > 0) {
                val chunk = minOf(remaining, capacity - writePos)
                src.copyInto(data, writePos, from, from + chunk)
                writePos = (writePos + chunk) % capacity
                size = minOf(capacity, size + chunk)
                from += chunk
                remaining -= chunk
            }
        }
    }

    fun snapshot(): ShortArray = synchronized(lock) {
        if (size == 0) return ShortArray(0)
        if (size < capacity) return data.copyOf(size)
        val out = ShortArray(capacity)
        val oldest = writePos
        data.copyInto(out, 0, oldest, capacity)
        if (oldest > 0) {
            data.copyInto(out, capacity - oldest, 0, oldest)
        }
        out
    }

    fun clear() {
        synchronized(lock) {
            writePos = 0
            size = 0
        }
    }

    companion object {
        const val DEFAULT_DURATION_MS = AudioBufferConfig.DEFAULT_SECONDS * 1_000
        const val MIN_DURATION_MS = AudioBufferConfig.MIN_SECONDS * 1_000
        const val MAX_DURATION_MS = AudioBufferConfig.MAX_SECONDS * 1_000
    }
}
