package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportAudioDto
import com.hibiki.data.arashi.model.ArashiExportContextDto
import com.hibiki.data.arashi.model.ArashiExportManifest
import com.hibiki.data.arashi.model.ArashiExportPhraseDto
import com.hibiki.data.arashi.model.ArashiExportType
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.PhraseSource
import com.hibiki.domain.model.StudyContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile

class ArashiExportJsonTest {
    @Test
    fun subjectIsOmittedWhenNull() {
        val dto = samplePhraseDto(subject = null)
        val encoded = ArashiExportJson.encodeManifest(
            ArashiExportManifest(
                schemaVersion = 1,
                sourceApp = "hibiki",
                exportId = "e1",
                exportedAt = "2026-08-23T08:30:00Z",
                exportType = ArashiExportType.PARTIAL,
                phrases = listOf(dto),
            ),
        )
        val decoded = ArashiExportJson.decodeManifest(encoded)
        assertNull(decoded.phrases.single().subject)
        assertEquals("generale", decoded.phrases.single().context.id)
    }
}

class ArashiExportPackagerTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun packageWithoutAudioContainsOnlyManifest() {
        val zip = temp.newFile("none.zip")
        ArashiExportPackager().write(zip, """{"ok":true}""", emptyMap())
        ZipFile(zip).use { archive ->
            val names = archive.entries().toList().map { it.name }
            assertEquals(listOf(ArashiExportContract.MANIFEST_FILE), names)
        }
    }

    @Test
    fun packageWithAudioDeduplicatesSharedFiles() {
        val audio = temp.newFile("clip.m4a")
        audio.writeBytes(byteArrayOf(1, 2, 3, 4))
        val zip = temp.newFile("with-audio.zip")
        ArashiExportPackager().write(
            zip,
            "{}",
            mapOf("sample-a.m4a" to audio, "sample-a.m4a" to audio),
        )
        ZipFile(zip).use { archive ->
            val names = archive.entries().toList().map { it.name }.toSet()
            assertEquals(
                setOf(ArashiExportContract.MANIFEST_FILE, "audio/sample-a.m4a"),
                names,
            )
            val audioEntry = archive.getEntry("audio/sample-a.m4a")
            assertEquals(4, audioEntry.size)
        }
    }
}

class ArashiExportMapperTest {
    @Test
    fun mapsNullableSubjectAndMissingAudio() {
        val phrase = Phrase(
            id = "p1",
            audioSampleId = "s1",
            contextId = "generale",
            subjectId = null,
            audioPath = "/missing.m4a",
            audioFingerprint = null,
            durationMs = 12,
            japaneseRaw = "やるじゃねえか！",
            japaneseCorrected = null,
            kana = "やるじゃねえか！",
            romaji = "Yaru ja nē ka!",
            literalTranslation = "letterale",
            naturalTranslation = "naturale",
            confidence = null,
            verified = true,
            source = PhraseSource.API,
            createdAt = 1_000,
            updatedAt = 2_000,
            transcriptionModel = "gpt-transcribe",
            analysisModel = "gpt-4o-mini",
            transcriptionPromptVersion = 1,
            analysisPromptVersion = 1,
        )
        val dto = ArashiExportMapper.toDto(
            phrase = phrase,
            context = StudyContext(
                id = "generale",
                name = "Generale",
                prompt = "",
                expectedLanguage = "ja",
                hasSubjects = false,
                isBuiltIn = true,
                sortOrder = 0,
            ),
            subject = null,
            audioFileName = ArashiExportMapper.audioFileName(phrase),
        )
        assertNull(dto.subject)
        assertFalse(dto.audio.included)
        assertNull(dto.audio.fileName)
        assertEquals("p1", dto.sourcePhraseId)
        assertEquals(ArashiExportTime.formatEpochMs(1_000), dto.createdAt)
        assertEquals(ArashiExportTime.formatEpochMs(2_000), dto.updatedAt)
        assertTrue(dto.verified)
        assertEquals("やるじゃねえか！", dto.japanese)
    }
}

private fun samplePhraseDto(subject: com.hibiki.data.arashi.model.ArashiExportSubjectDto?) =
    ArashiExportPhraseDto(
        sourcePhraseId = "p1",
        context = ArashiExportContextDto(id = "generale", name = "Generale"),
        subject = subject,
        japanese = "こんにちは",
        kana = "こんにちは",
        romaji = "konnichiwa",
        literalTranslation = "ciao",
        naturalTranslation = "ciao",
        verified = false,
        updatedAt = "2026-08-23T08:30:00Z",
        createdAt = "2026-08-23T08:00:00Z",
        audio = ArashiExportAudioDto(included = false),
    )
