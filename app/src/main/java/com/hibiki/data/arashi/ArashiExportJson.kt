package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportManifest
import com.hibiki.data.arashi.model.ArashiImportResultDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ArashiExportJson {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encodeManifest(manifest: ArashiExportManifest): String =
        json.encodeToString(ArashiExportManifest.serializer(), manifest)

    fun decodeManifest(content: String): ArashiExportManifest =
        json.decodeFromString(ArashiExportManifest.serializer(), content)

    fun encodeResult(result: ArashiImportResultDto): String =
        json.encodeToString(ArashiImportResultDto.serializer(), result)

    fun decodeResult(content: String): ArashiImportResultDto =
        json.decodeFromString(ArashiImportResultDto.serializer(), content)
}
