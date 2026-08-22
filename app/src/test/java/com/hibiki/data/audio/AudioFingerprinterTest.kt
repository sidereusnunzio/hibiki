package com.hibiki.data.audio

import com.hibiki.domain.model.AudioMatchConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class AudioFingerprinterTest {
    @Test
    fun similarClipsMatchAboveConservativeThreshold() {
        val base = sineClip(frequency = 440.0, durationMs = 900, startPadMs = 40)
        val shifted = sineClip(frequency = 440.0, durationMs = 900, startPadMs = 90)
        val a = AudioFingerprinter.compute(base)
        val b = AudioFingerprinter.compute(shifted)
        val similarity = AudioFingerprinter.similarity(a, b)
        assertTrue("expected high similarity, was $similarity", similarity >= AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD)
    }

    @Test
    fun differentClipsStayBelowThreshold() {
        val a = AudioFingerprinter.compute(sineClip(frequency = 440.0, durationMs = 900, startPadMs = 40))
        val b = AudioFingerprinter.compute(sineClip(frequency = 880.0, durationMs = 900, startPadMs = 40))
        val similarity = AudioFingerprinter.similarity(a, b)
        assertTrue("expected lower similarity, was $similarity", similarity < AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD)
    }

    @Test
    fun similarDurationsAreCompatible() {
        assertTrue(AudioFingerprinter.durationCompatible(2000, 2100))
        assertTrue(AudioFingerprinter.durationCompatible(800, 1000))
    }

    @Test
    fun farDurationsAreRejected() {
        org.junit.Assert.assertFalse(AudioFingerprinter.durationCompatible(2000, 4000))
        org.junit.Assert.assertFalse(AudioFingerprinter.durationCompatible(500, 2000))
    }

    private fun sineClip(frequency: Double, durationMs: Int, startPadMs: Int): PcmClip {
        val sampleRate = 16_000
        val total = sampleRate * (durationMs + startPadMs) / 1000
        val pad = sampleRate * startPadMs / 1000
        val samples = ShortArray(total)
        for (i in pad until total) {
            val t = (i - pad) / sampleRate.toDouble()
            samples[i] = (sin(2 * Math.PI * frequency * t) * 20_000).toInt().toShort()
        }
        return PcmClip(samples, sampleRate)
    }
}
