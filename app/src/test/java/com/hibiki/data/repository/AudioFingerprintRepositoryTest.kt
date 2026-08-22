package com.hibiki.data.repository

import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.AudioMatchConfig
import com.hibiki.domain.model.AudioSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AudioFingerprintRepositoryTest {
    private val repository = AudioFingerprintRepository()
    private val fingerprint = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    @Test
    fun matchesSameClipWithCompatibleDuration() {
        val sample = sample(durationMs = 2000)
        val match = repository.findBestMatch(
            fingerprint = fingerprint,
            candidates = listOf(sample),
            threshold = AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD,
            durationMs = 2100,
        )
        assertNotNull(match)
        assertEquals(sample.id, match?.sample?.id)
    }

    @Test
    fun rejectsCompatibleFingerprintWhenDurationDiffersTooMuch() {
        val sample = sample(durationMs = 4000)
        val match = repository.findBestMatch(
            fingerprint = fingerprint,
            candidates = listOf(sample),
            threshold = AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD,
            durationMs = 1000,
        )
        assertNull(match)
    }

    private fun sample(durationMs: Long) = AudioSample(
        id = "sample-1",
        audioPath = null,
        audioFingerprint = fingerprint,
        durationMs = durationMs,
        japaneseRaw = "やるじゃねえか！",
        confidence = null,
        transcriptionModel = "gpt-transcribe",
        transcriptionPromptVersion = DefaultPrompts.TRANSCRIPTION_PROMPT_VERSION,
        createdAt = 0L,
    )
}
