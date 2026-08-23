package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportAudioDto
import com.hibiki.data.arashi.model.ArashiExportContextDto
import com.hibiki.data.arashi.model.ArashiExportPhraseDto
import com.hibiki.data.arashi.model.ArashiExportSubjectDto
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import java.io.File

data class ArashiExportAudioRef(
    val fileName: String,
    val sourceFile: File,
)

object ArashiExportMapper {
    fun toDto(
        phrase: Phrase,
        context: StudyContext?,
        subject: Subject?,
        audioFileName: String?,
    ): ArashiExportPhraseDto {
        return ArashiExportPhraseDto(
            sourcePhraseId = phrase.id,
            context = ArashiExportContextDto(
                id = context?.id ?: phrase.contextId,
                name = context?.name ?: phrase.contextId,
            ),
            subject = subject?.let {
                ArashiExportSubjectDto(
                    id = it.id,
                    displayName = it.displayName,
                    japaneseName = it.japaneseName,
                )
            },
            japanese = phrase.japaneseDisplay,
            kana = phrase.kana,
            romaji = phrase.romaji,
            literalTranslation = phrase.literalTranslation,
            naturalTranslation = phrase.naturalTranslation,
            verified = phrase.verified,
            updatedAt = ArashiExportTime.formatEpochMs(phrase.updatedAt),
            createdAt = ArashiExportTime.formatEpochMs(phrase.createdAt),
            audio = ArashiExportAudioDto(
                included = audioFileName != null,
                fileName = audioFileName,
            ),
        )
    }

    fun audioFileName(phrase: Phrase): String? {
        val path = phrase.audioPath?.takeIf { it.isNotBlank() } ?: return null
        val file = File(path)
        if (!file.isFile) return null
        val ext = file.extension.ifBlank { "m4a" }
        return "${phrase.audioSampleId}.$ext"
    }

    fun audioRef(phrase: Phrase): ArashiExportAudioRef? {
        val fileName = audioFileName(phrase) ?: return null
        val file = File(phrase.audioPath!!)
        return ArashiExportAudioRef(fileName = fileName, sourceFile = file)
    }
}
