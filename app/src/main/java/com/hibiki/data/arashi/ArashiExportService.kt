package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportManifest
import com.hibiki.data.arashi.model.ArashiExportType
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.local.toModel
import com.hibiki.data.repository.PhraseRepository
import java.io.File
import java.util.UUID

data class ArashiExportPackage(
    val file: File,
    val exportId: String,
    val exportType: ArashiExportType,
    val phraseCount: Int,
    val exportedAtEpochMs: Long,
    val exportedPhraseIds: List<String>,
)

class ArashiExportService(
    private val phraseRepository: PhraseRepository,
    private val databaseProvider: HibikiDatabaseProvider,
    private val packager: ArashiExportPackager = ArashiExportPackager(),
) {
    suspend fun createPackage(
        outputFile: File,
        phraseIds: List<String>? = null,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): ArashiExportPackage {
        val allPhrases = phraseRepository.getAll()
        val selected = when {
            phraseIds != null -> {
                val requested = phraseIds.toSet()
                allPhrases.filter { it.id in requested }
            }
            else -> ArashiExportSelector.selectPending(allPhrases)
        }
        if (selected.isEmpty()) {
            throw ArashiExportException.NothingToExport
        }

        val db = databaseProvider.getDatabase()
        val contexts = db.contextDao().getAll().associateBy { it.id }
        val subjects = db.subjectDao().getAll().associateBy { it.id }

        val audioFiles = linkedMapOf<String, File>()
        val dtos = selected.map { phrase ->
            val audio = ArashiExportMapper.audioRef(phrase)
            if (audio != null) {
                audioFiles.putIfAbsent(audio.fileName, audio.sourceFile)
            }
            ArashiExportMapper.toDto(
                phrase = phrase,
                context = contexts[phrase.contextId]?.toModel(),
                subject = phrase.subjectId?.let { subjects[it]?.toModel() },
                audioFileName = audio?.fileName,
            )
        }

        val exportId = UUID.randomUUID().toString()
        val exportType = ArashiExportType.PARTIAL
        val manifest = ArashiExportManifest(
            schemaVersion = ArashiExportContract.SCHEMA_VERSION,
            sourceApp = ArashiExportContract.SOURCE_APP,
            exportId = exportId,
            exportedAt = ArashiExportTime.formatEpochMs(nowEpochMs),
            exportType = exportType,
            phrases = dtos,
        )
        packager.write(outputFile, ArashiExportJson.encodeManifest(manifest), audioFiles)
        return ArashiExportPackage(
            file = outputFile,
            exportId = exportId,
            exportType = exportType,
            phraseCount = dtos.size,
            exportedAtEpochMs = nowEpochMs,
            exportedPhraseIds = selected.map { it.id },
        )
    }
}

sealed class ArashiExportException(message: String) : Exception(message) {
    data object NothingToExport : ArashiExportException("Nessuna frase da sincronizzare con Arashi")
}
