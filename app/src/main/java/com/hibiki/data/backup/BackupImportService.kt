package com.hibiki.data.backup

import android.content.Context
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.media.MediaDirectories
import com.hibiki.data.media.UmamusumePortraitSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.zip.ZipFile

class BackupImportService(
    private val context: Context,
    private val databaseProvider: HibikiDatabaseProvider,
    private val mediaDirectories: MediaDirectories,
    private val umamusumePortraitSeeder: UmamusumePortraitSeeder,
    private val validator: BackupValidator = BackupValidator(),
) {
    fun import(archiveFile: File): Flow<BackupImportState> = flow {
        emit(BackupImportState.Preparing)
        if (!archiveFile.exists()) {
            throw BackupValidationException.MissingFile(archiveFile.path)
        }
        if (archiveFile.length() > BackupConstants.MAX_BACKUP_SIZE_BYTES) {
            throw BackupValidationException.SecurityViolation("Archive exceeds max size")
        }

        val tempRoot = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        val safetyBackup = File(context.cacheDir, "backup_safety_${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        safetyBackup.mkdirs()
        var rollbackNeeded = false

        try {
            emit(BackupImportState.Extracting)
            extractZipSafely(archiveFile, tempRoot)

            emit(BackupImportState.ReadingManifest)
            val manifestFile = File(tempRoot, BackupConstants.MANIFEST_FILE)
            if (!manifestFile.exists()) {
                throw BackupValidationException.MissingFile(BackupConstants.MANIFEST_FILE)
            }
            val manifest = validator.parseManifest(manifestFile.readText())
            validator.validateManifest(manifest)

            emit(BackupImportState.ValidatingChecksums)
            validateExtractedFiles(tempRoot, manifest)

            val importedDb = File(tempRoot, manifest.databaseFile)
            if (!importedDb.exists()) {
                throw BackupValidationException.MissingFile(manifest.databaseFile)
            }

            emit(BackupImportState.ValidatingDatabase)
            val dbValidation = databaseProvider.validateDatabaseFile(importedDb)
            if (!dbValidation.isValid) {
                throw BackupValidationException.DatabaseInvalid(dbValidation.message)
            }

            emit(BackupImportState.BackingUpCurrentData)
            rollbackNeeded = true
            backupCurrentData(safetyBackup)

            emit(BackupImportState.ReplacingDatabase)
            databaseProvider.closeDatabase()
            databaseProvider.replaceDatabase(importedDb)

            emit(BackupImportState.ReplacingMedia)
            val importedMedia = File(tempRoot, BackupConstants.MEDIA_DIR)
            if (importedMedia.exists()) {
                mediaDirectories.replaceFromBackupMedia(importedMedia)
            } else {
                mediaDirectories.clear()
            }

            emit(BackupImportState.ReopeningDatabase)
            databaseProvider.reopenDatabase()

            emit(BackupImportState.FinalValidation)
            val finalValidation = databaseProvider.validateDatabaseFile(databaseProvider.getDatabaseFile())
            if (!finalValidation.isValid) {
                throw BackupValidationException.DatabaseInvalid(finalValidation.message)
            }

            umamusumePortraitSeeder.ensureInstalled()

            safetyBackup.deleteRecursively()
            rollbackNeeded = false
            emit(BackupImportState.Completed)
        } catch (e: Exception) {
            if (rollbackNeeded) {
                emit(BackupImportState.RollingBack)
                try {
                    restoreFromSafetyBackup(safetyBackup)
                } catch (rollbackError: Exception) {
                    emit(BackupImportState.Failed(rollbackError))
                    return@flow
                }
            }
            emit(BackupImportState.Failed(e))
        } finally {
            tempRoot.deleteRecursively()
            if (!rollbackNeeded) {
                safetyBackup.deleteRecursively()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractZipSafely(archive: File, destRoot: File) {
        var totalUncompressed = 0L
        var fileCount = 0
        ZipFile(archive).use { zip ->
            zip.entries().asIterator().forEach { entry ->
                validator.validateRelativeArchivePath(entry.name)
                if (entry.isDirectory) return@forEach
                fileCount++
                if (fileCount > BackupConstants.MAX_FILE_COUNT) {
                    throw BackupValidationException.SecurityViolation("Too many files in archive")
                }
                totalUncompressed += entry.size
                if (totalUncompressed > BackupConstants.MAX_UNCOMPRESSED_SIZE_BYTES) {
                    throw BackupValidationException.SecurityViolation("Decompression bomb detected")
                }
                if (entry.size > BackupConstants.MAX_SINGLE_FILE_SIZE_BYTES) {
                    throw BackupValidationException.SecurityViolation("Entry too large: ${entry.name}")
                }
                val target = validator.resolveSafeExtractPath(destRoot, entry.name)
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun validateExtractedFiles(root: File, manifest: com.hibiki.data.backup.model.BackupManifest) {
        manifest.files.forEach { entry ->
            val file = File(root, entry.relativePath)
            if (!file.exists()) {
                throw BackupValidationException.MissingFile(entry.relativePath)
            }
            if (file.length() != entry.sizeBytes) {
                throw BackupValidationException.ChecksumMismatch(entry.relativePath)
            }
            val sha = mediaDirectories.sha256(file)
            if (sha != entry.sha256) {
                throw BackupValidationException.ChecksumMismatch(entry.relativePath)
            }
        }
    }

    private fun backupCurrentData(safetyDir: File) {
        val dbFile = databaseProvider.getDatabaseFile()
        if (dbFile.exists()) {
            val target = File(safetyDir, BackupConstants.DATABASE_DIR)
            target.mkdirs()
            dbFile.copyTo(File(target, BackupConstants.DATABASE_FILE_NAME), overwrite = true)
            databaseProvider.deleteDatabaseSidecars(dbFile)
        }
        val mediaBackup = File(safetyDir, BackupConstants.MEDIA_DIR)
        mediaDirectories.copyToBackupMedia(mediaBackup)
    }

    private fun restoreFromSafetyBackup(safetyDir: File) {
        databaseProvider.closeDatabase()
        val dbBackup = File(safetyDir, "${BackupConstants.DATABASE_DIR}/${BackupConstants.DATABASE_FILE_NAME}")
        if (dbBackup.exists()) {
            databaseProvider.replaceDatabase(dbBackup)
        }
        val mediaBackup = File(safetyDir, BackupConstants.MEDIA_DIR)
        if (mediaBackup.exists()) {
            mediaDirectories.replaceFromBackupMedia(mediaBackup)
        } else {
            mediaDirectories.clear()
        }
        databaseProvider.reopenDatabase()
    }
}
