package com.hibiki.data.backup

import com.hibiki.data.backup.model.BackupManifest
import com.hibiki.data.backup.model.BackupManifestFile
import com.hibiki.data.backup.model.BackupMediaType
import com.hibiki.data.local.DatabaseConstants
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {
    private val validator = BackupValidator()

    @Test
    fun rejectsAbsolutePath() {
        assertThrows(BackupValidationException.SecurityViolation::class.java) {
            validator.validateRelativeArchivePath("/absolute/path")
        }
    }

    @Test
    fun rejectsPathTraversal() {
        assertThrows(BackupValidationException.SecurityViolation::class.java) {
            validator.validateRelativeArchivePath("media/../../etc/passwd")
        }
    }

    private fun sampleManifest(schemaVersion: Int, formatName: String = BackupConstants.FORMAT_NAME) =
        BackupManifest(
            formatName = formatName,
            formatVersion = BackupConstants.FORMAT_VERSION,
            databaseSchemaVersion = schemaVersion,
            appVersion = "1.0.00",
            createdAt = System.currentTimeMillis(),
            databaseFile = "database/hibiki.db",
            databaseSha256 = "abc",
            mediaFileCount = 0,
            totalUncompressedBytes = 100,
            files = listOf(
                BackupManifestFile(
                    relativePath = "database/hibiki.db",
                    sizeBytes = 100,
                    sha256 = "abc",
                    mediaType = BackupMediaType.DATABASE,
                ),
            ),
        )

    @Test
    fun rejectsNewerSchema() {
        val manifest = sampleManifest(DatabaseConstants.SCHEMA_VERSION + 1)
        assertThrows(BackupValidationException.UnsupportedSchema::class.java) {
            validator.validateManifest(manifest)
        }
    }

    @Test
    fun rejectsUnknownFormat() {
        val manifest = sampleManifest(DatabaseConstants.SCHEMA_VERSION, formatName = "unknown")
        assertThrows(BackupValidationException.UnsupportedFormat::class.java) {
            validator.validateManifest(manifest)
        }
    }

    @Test
    fun parseManifest_validJson() {
        val json = """
            {
              "formatName": "hibikibackup",
              "formatVersion": 1,
              "databaseSchemaVersion": 1,
              "appVersion": "1.0.00",
              "createdAt": 1000,
              "databaseFile": "database/hibiki.db",
              "databaseSha256": "abc",
              "mediaFileCount": 0,
              "totalUncompressedBytes": 0,
              "files": []
            }
        """.trimIndent()
        val manifest = validator.parseManifest(json)
        assertTrue(manifest.formatName == BackupConstants.FORMAT_NAME)
    }
}
