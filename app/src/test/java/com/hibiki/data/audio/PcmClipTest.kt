package com.hibiki.data.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmClipTest {
    @Test
    fun clipsShorterThan1000msAreEmpty() {
        val clip = PcmClip(ShortArray(16_000 * 999 / 1000), sampleRate = 16_000)
        assertTrue(clip.isEmpty)
    }

    @Test
    fun clipsOfAtLeast1000msAreNotEmpty() {
        val clip = PcmClip(ShortArray(16_000), sampleRate = 16_000)
        assertFalse(clip.isEmpty)
    }
}
