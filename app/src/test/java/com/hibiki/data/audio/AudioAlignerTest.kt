package com.hibiki.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAlignerTest {
    @Test
    fun findsMidSegmentDespiteDifferentPaddingAndDuration() {
        val core = sineWave(frequencyHz = 440, sampleRate = 16_000, durationMs = 900)
        val reference = concat(silence(16_000, 50), core, silence(16_000, 80))
        val query = concat(silence(16_000, 400), core, silence(16_000, 1200))
        val result = AudioAligner.findMidSegmentMatch(
            newClip = PcmClip(query, 16_000),
            referenceClip = PcmClip(reference, 16_000),
        )
        assertNotNull(result)
        assertTrue(
            "expected high mid-segment score, got ${result!!.alignmentScore}",
            result.alignmentScore >= 0.88f,
        )
    }

    @Test
    fun doesNotRejectWhenDurationRatioIsLargeIfMidSegmentMatches() {
        val core = sineWave(frequencyHz = 660, sampleRate = 16_000, durationMs = 700)
        val shortArchive = concat(silence(16_000, 40), core, silence(16_000, 40))
        val longCapture = concat(silence(16_000, 800), core, silence(16_000, 2500))
        val result = AudioAligner.findMidSegmentMatch(
            newClip = PcmClip(longCapture, 16_000),
            referenceClip = PcmClip(shortArchive, 16_000),
        )
        assertNotNull(result)
        assertTrue(
            "expected high mid-segment score, got ${result!!.alignmentScore}",
            result.alignmentScore >= 0.88f,
        )
    }

    @Test
    fun rejectsUnrelatedAudio() {
        val a = sineWave(frequencyHz = 440, sampleRate = 16_000, durationMs = 1000)
        val b = sineWave(frequencyHz = 1200, sampleRate = 16_000, durationMs = 1000)
        val result = AudioAligner.findMidSegmentMatch(
            newClip = PcmClip(a, 16_000),
            referenceClip = PcmClip(b, 16_000),
        )
        assertTrue(result == null || result.alignmentScore < 0.88f)
    }

    @Test
    fun probeCentersOnActiveRegionNotTrailingSilence() {
        val core = ShortArray(3_200) { 8_000 }
        val samples = concat(silence(16_000, 500), core, silence(16_000, 2000))
        val center = AudioAligner.activeRegionCenter(samples, sampleRate = 16_000)
        val coreStart = (16_000 * 500 / 1000)
        val coreEnd = coreStart + core.size
        assertTrue("center=$center expected inside active core", center in coreStart until coreEnd)
        val probe = AudioAligner.extractMidProbe(samples, sampleRate = 16_000)
        assertNotNull(probe)
        assertTrue(probe!!.any { abs(it.toInt()) > 1000 })
        assertEquals(16_000, probe.size)
    }

    @Test
    fun probeIs1000MsForClipsUpTo10s() {
        val samples = ShortArray(16_000 * 10) { 8_000 }
        val probe = AudioAligner.extractMidProbe(samples, sampleRate = 16_000)
        assertNotNull(probe)
        assertEquals(16_000, probe!!.size)
    }

    @Test
    fun probeIs2000MsForClipsLongerThan10s() {
        val samples = ShortArray(16_000 * 11) { 8_000 }
        val probe = AudioAligner.extractMidProbe(samples, sampleRate = 16_000)
        assertNotNull(probe)
        assertEquals(32_000, probe!!.size)
    }

    private fun abs(value: Int): Int = if (value < 0) -value else value

    private fun sineWave(frequencyHz: Int, sampleRate: Int, durationMs: Long): ShortArray {
        val count = (sampleRate * durationMs / 1000).toInt()
        return ShortArray(count) { index ->
            val t = index.toDouble() / sampleRate
            (kotlin.math.sin(2.0 * kotlin.math.PI * frequencyHz * t) * 20_000).toInt().toShort()
        }
    }

    private fun silence(sampleRate: Int, durationMs: Long): ShortArray =
        ShortArray((sampleRate * durationMs / 1000).toInt())

    private fun concat(vararg parts: ShortArray): ShortArray {
        val total = parts.sumOf { it.size }
        val out = ShortArray(total)
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }
}
