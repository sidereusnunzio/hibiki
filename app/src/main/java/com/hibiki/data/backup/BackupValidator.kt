package com.hibiki.data.backup

import com.hibiki.data.backup.model.BackupManifest
import com.hibiki.data.local.DatabaseConstants
import kotlinx.serialization.json.Json

sealed class BackupValidationException(message: String) : Exception(message) {
    class UnsupportedFormat(message: String) : BackupValidationException(message)
    class UnsupportedSchema(message: String) : BackupValidationException(message)
    class ChecksumMismatch(path: String) : BackupValidationException("Checksum mismatch: $path")
    class MissingFile(path: String) : BackupValidationException("Missing file: $path")
    class SecurityViolation(message: String) : BackupValidationException(message)
    class DatabaseInvalid(message: String) : BackupValidationException(message)
    class ManifestInvalid(message: String) : BackupValidationException(message)
}

class BackupValidator(
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    fun parseManifest(content: String): BackupManifest {
        return try {
            json.decodeFromString(BackupManifest.serializer(), content)
        } catch (e: Exception) {
            throw BackupValidationException.ManifestInvalid(e.message ?: "Invalid manifest JSON")
        }
    }

    fun validateManifest(manifest: BackupManifest) {
        if (manifest.formatName != BackupConstants.FORMAT_NAME) {
            throw BackupValidationException.UnsupportedFormat(
                "Unknown format: ${manifest.formatName}",
            )
        }
        if (manifest.formatVersion > BackupConstants.FORMAT_VERSION) {
            throw BackupValidationException.UnsupportedFormat(
                "Backup format ${manifest.formatVersion} is newer than supported ${BackupConstants.FORMAT_VERSION}",
            )
        }
        if (manifest.databaseSchemaVersion > DatabaseConstants.SCHEMA_VERSION) {
            throw BackupValidationException.UnsupportedSchema(
                "Database schema ${manifest.databaseSchemaVersion} is newer than app schema ${DatabaseConstants.SCHEMA_VERSION}",
            )
        }
        if (manifest.files.isEmpty()) {
            throw BackupValidationException.ManifestInvalid("Manifest contains no files")
        }
        if (manifest.files.size > BackupConstants.MAX_FILE_COUNT) {
            throw BackupValidationException.SecurityViolation("Too many files in manifest")
        }
        if (manifest.totalUncompressedBytes > BackupConstants.MAX_UNCOMPRESSED_SIZE_BYTES) {
            throw BackupValidationException.SecurityViolation("Uncompressed size exceeds limit")
        }
        manifest.files.forEach { file ->
            validateRelativeArchivePath(file.relativePath)
            if (file.sizeBytes > BackupConstants.MAX_SINGLE_FILE_SIZE_BYTES) {
                throw BackupValidationException.SecurityViolation(
                    "File too large: ${file.relativePath}",
                )
            }
        }
    }

    fun validateRelativeArchivePath(path: String) {
        if (path.isBlank()) {
            throw BackupValidationException.SecurityViolation("Empty archive path")
        }
        if (path.startsWith("/") || path.startsWith("\\")) {
            throw BackupValidationException.SecurityViolation("Absolute path not allowed: $path")
        }
        if (path.contains("..")) {
            throw BackupValidationException.SecurityViolation("Path traversal not allowed: $path")
        }
    }

    fun resolveSafeExtractPath(extractRoot: java.io.File, entryName: String): java.io.File {
        validateRelativeArchivePath(entryName)
        val target = java.io.File(extractRoot, entryName.replace('\\', '/')).canonicalFile
        val root = extractRoot.canonicalFile
        if (!target.path.startsWith(root.path)) {
            throw BackupValidationException.SecurityViolation("Extract path escapes root: $entryName")
        }
        return target
    }
}
