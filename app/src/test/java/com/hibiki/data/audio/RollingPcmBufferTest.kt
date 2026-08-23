package com.hibiki.data.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RollingPcmBufferTest {
    @Test
    fun snapshotMatchesWritesBeforeCapacity() {
        val buffer = RollingPcmBuffer(sampleRate = 8, durationMs = 1_000)
        buffer.write(shortArrayOf(1, 2, 3, 4), 0, 4)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), buffer.snapshot())
    }

    @Test
    fun snapshotKeepsOnlyTheMostRecentWindow() {
        val buffer = RollingPcmBuffer(sampleRate = 4, durationMs = 1_000)
        buffer.write((0..11).map { it.toShort() }.toShortArray(), 0, 12)
        assertArrayEquals(shortArrayOf(8, 9, 10, 11), buffer.snapshot())
    }

    @Test
    fun wrapAroundPreservesOrder() {
        val buffer = RollingPcmBuffer(sampleRate = 4, durationMs = 1_000)
        buffer.write(shortArrayOf(1, 2, 3), 0, 3)
        buffer.write(shortArrayOf(4, 5, 6), 0, 3)
        assertArrayEquals(shortArrayOf(3, 4, 5, 6), buffer.snapshot())
    }

    @Test
    fun twoSecondWindowAtDefaultRate() {
        val rate = 44_100
        val buffer = RollingPcmBuffer(sampleRate = rate)
        val extra = 50
        val total = rate * 2 + extra
        val samples = ShortArray(total) { (it % 1000).toShort() }
        buffer.write(samples, 0, samples.size)
        val snapshot = buffer.snapshot()
        assertEquals(rate * 2, snapshot.size)
        assertEquals((extra % 1000).toShort(), snapshot.first())
        assertEquals(((total - 1) % 1000).toShort(), snapshot.last())
    }

    @Test
    fun clearEmptiesTheWindow() {
        val buffer = RollingPcmBuffer(sampleRate = 8, durationMs = 1_000)
        buffer.write(shortArrayOf(1, 2, 3), 0, 3)
        buffer.clear()
        assertEquals(0, buffer.snapshot().size)
    }
}
