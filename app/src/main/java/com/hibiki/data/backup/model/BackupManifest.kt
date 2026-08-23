package com.hibiki.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
enum class BackupMediaType {
    DATABASE,
    AUDIO,
    IMAGE,
    OTHER,
}

@Serializable
data class BackupManifestFile(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val mediaType: BackupMediaType,
)

@Serializable
data class BackupManifest(
    val formatName: String,
    val formatVersion: Int,
    val databaseSchemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val databaseFile: String,
    val databaseSha256: String,
    val mediaFileCount: Int,
    val totalUncompressedBytes: Long,
    val files: List<BackupManifestFile>,
)
