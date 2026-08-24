package com.hibiki.data.repository

import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.PhraseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFingerprintRepositoryTest {
    private val repository = AudioFingerprintRepository()
    private val fingerprint = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    @Test
    fun retrieveCandidatesReturnsTopMatchesAboveThreshold() {
        val strong = target("strong", fingerprint)
        val weak = target("weak", ByteArray(8) { 0xFF.toByte() })
        val candidates = repository.retrieveCandidates(
            queryFingerprints = listOf(fingerprint),
            targets = listOf(strong, weak),
            threshold = 0.90f,
        )
        assertEquals(1, candidates.size)
        assertEquals("strong", candidates.first().target.phrase.id)
        assertTrue(candidates.first().fingerprintScore >= 0.90f)
    }

    @Test
    fun retrieveCandidatesRespectsTopN() {
        val targets = (1..8).map { index ->
            target("p$index", fingerprint.copyOf().also { it[0] = index.toByte() })
        }
        val candidates = repository.retrieveCandidates(
            queryFingerprints = listOf(fingerprint),
            targets = targets,
            topN = 3,
        )
        assertEquals(3, candidates.size)
    }

    private fun target(id: String, fp: ByteArray) = PhraseMatchTarget(
        phrase = Phrase(
            id = id,
            audioSampleId = "sample-$id",
            contextId = "ctx",
            subjectId = null,
            audioPath = null,
            audioFingerprint = fp,
            durationMs = 2000,
            japaneseRaw = "やるじゃねえか！",
            japaneseCorrected = null,
            kana = "やるじゃねえか",
            romaji = "yaru ja nee ka",
            literalTranslation = "fare, eh?",
            naturalTranslation = "Not bad!",
            confidence = null,
            verified = false,
            source = PhraseSource.API,
            createdAt = 0L,
            updatedAt = 0L,
            transcriptionModel = "gpt-transcribe",
            analysisModel = "gpt-4o-mini",
            transcriptionPromptVersion = 4,
            analysisPromptVersion = 1,
        ),
        prototypeId = "$id:p0",
        fingerprint = fp,
        durationMs = 2000,
        pcmPreview = null,
        audioPath = null,
    )
}
