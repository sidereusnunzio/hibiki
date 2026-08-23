package com.hibiki.data.arashi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ArashiExportType {
    @SerialName("partial")
    PARTIAL,

    @SerialName("full")
    FULL,
}

@Serializable
data class ArashiExportManifest(
    val schemaVersion: Int,
    val sourceApp: String,
    val exportId: String,
    val exportedAt: String,
    val exportType: ArashiExportType,
    val phrases: List<ArashiExportPhraseDto>,
)

@Serializable
data class ArashiExportPhraseDto(
    val sourcePhraseId: String,
    val context: ArashiExportContextDto,
    val subject: ArashiExportSubjectDto? = null,
    val japanese: String,
    val kana: String,
    val romaji: String,
    val literalTranslation: String,
    val naturalTranslation: String,
    val verified: Boolean,
    val updatedAt: String,
    val createdAt: String,
    val audio: ArashiExportAudioDto,
)

@Serializable
data class ArashiExportContextDto(
    val id: String,
    val name: String,
)

@Serializable
data class ArashiExportSubjectDto(
    val id: String,
    val displayName: String,
    val japaneseName: String,
)

@Serializable
data class ArashiExportAudioDto(
    val included: Boolean,
    val fileName: String? = null,
)

@Serializable
data class ArashiImportResultDto(
    val exportId: String,
    val received: Int,
    val imported: Int,
    val updated: Int,
    val duplicates: Int,
    val failed: Int,
)
