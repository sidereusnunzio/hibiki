package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportType
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.PhraseSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArashiExportSelectorTest {
    @Test
    fun fullExportSelectsEveryPhrase() {
        val phrases = listOf(phrase("a", updatedAt = 10), phrase("b", updatedAt = 50))
        val selected = ArashiExportSelector.select(phrases, ArashiExportType.FULL, lastSuccessfulExportAtMs = 20)
        assertEquals(listOf("a", "b"), selected.map { it.id })
    }

    @Test
    fun partialExportSelectsOnlyNewerThanLastSuccess() {
        val phrases = listOf(
            phrase("old", updatedAt = 10),
            phrase("equal", updatedAt = 20),
            phrase("new", updatedAt = 21),
        )
        val selected = ArashiExportSelector.select(
            phrases,
            ArashiExportType.PARTIAL,
            lastSuccessfulExportAtMs = 20,
        )
        assertEquals(listOf("new"), selected.map { it.id })
    }

    @Test
    fun partialWithoutPreviousExportSendsEverything() {
        val phrases = listOf(phrase("a", updatedAt = 1), phrase("b", updatedAt = 2))
        val selected = ArashiExportSelector.select(
            phrases,
            ArashiExportType.PARTIAL,
            lastSuccessfulExportAtMs = null,
        )
        assertEquals(listOf("a", "b"), selected.map { it.id })
        assertEquals(
            ArashiExportType.FULL,
            ArashiExportSelector.effectiveType(ArashiExportType.PARTIAL, lastSuccessfulExportAtMs = null),
        )
    }
}

class ArashiExportTimeTest {
    @Test
    fun formatsComparableUtcTimestamps() {
        val epochMs = Instant.parse("2026-08-23T08:30:00Z").toEpochMilli()
        val iso = ArashiExportTime.formatEpochMs(epochMs)
        assertEquals("2026-08-23T08:30:00Z", iso)
        assertEquals(epochMs, ArashiExportTime.parseToEpochMs(iso))
    }
}

class ArashiExportResultInterpreterTest {
    @Test
    fun successOnlyWhenResultIsValidAndExportIdMatches() {
        val json = ArashiExportJson.encodeResult(
            com.hibiki.data.arashi.model.ArashiImportResultDto(
                exportId = "exp-1",
                received = 3,
                imported = 1,
                updated = 1,
                duplicates = 1,
                failed = 0,
            ),
        )
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = ArashiExportContract.RESULT_OK,
            resultJson = json,
            errorMessage = null,
            expectedExportId = "exp-1",
        )
        assertTrue(outcome is ArashiExportOutcome.Success)
    }

    @Test
    fun cancelDoesNotCountAsSuccess() {
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = ArashiExportContract.RESULT_CANCELED,
            resultJson = null,
            errorMessage = null,
            expectedExportId = "exp-1",
        )
        assertEquals(ArashiExportOutcome.Cancelled, outcome)
    }

    @Test
    fun completeFailureErrorDoesNotCountAsSuccess() {
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = ArashiExportContract.RESULT_CANCELED,
            resultJson = null,
            errorMessage = "schemaVersion non supportata",
            expectedExportId = "exp-1",
        )
        assertTrue(outcome is ArashiExportOutcome.Invalid)
    }

    @Test
    fun exportIdMismatchIsInvalid() {
        val json = ArashiExportJson.encodeResult(
            com.hibiki.data.arashi.model.ArashiImportResultDto(
                exportId = "other",
                received = 1,
                imported = 1,
                updated = 0,
                duplicates = 0,
                failed = 0,
            ),
        )
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = ArashiExportContract.RESULT_OK,
            resultJson = json,
            errorMessage = null,
            expectedExportId = "exp-1",
        )
        val invalid = outcome as ArashiExportOutcome.Invalid
        assertTrue(invalid.message.contains("exportId"))
    }

    @Test
    fun malformedJsonIsInvalid() {
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = ArashiExportContract.RESULT_OK,
            resultJson = "{not-json",
            errorMessage = null,
            expectedExportId = "exp-1",
        )
        assertTrue(outcome is ArashiExportOutcome.Invalid)
    }
}

private fun phrase(id: String, updatedAt: Long) = Phrase(
    id = id,
    audioSampleId = "sample-$id",
    contextId = "generale",
    subjectId = null,
    audioPath = null,
    audioFingerprint = null,
    durationMs = 0,
    japaneseRaw = "テスト",
    japaneseCorrected = null,
    kana = "てすと",
    romaji = "tesuto",
    literalTranslation = "test",
    naturalTranslation = "test",
    confidence = null,
    verified = false,
    source = PhraseSource.API,
    createdAt = updatedAt,
    updatedAt = updatedAt,
    transcriptionModel = "gpt-transcribe",
    analysisModel = "gpt-4o-mini",
    transcriptionPromptVersion = 1,
    analysisPromptVersion = 1,
)
