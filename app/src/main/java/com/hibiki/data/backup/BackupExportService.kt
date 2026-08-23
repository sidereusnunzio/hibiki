package com.hibiki.data.backup

import android.content.Context
import com.hibiki.BuildConfig
import com.hibiki.data.backup.model.BackupManifest
import com.hibiki.data.backup.model.BackupManifestFile
import com.hibiki.data.backup.model.BackupMediaType
import com.hibiki.data.local.DatabaseConstants
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.media.MediaDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BackupExportService(
    private val context: Context,
    private val databaseProvider: HibikiDatabaseProvider,
    private val mediaDirectories: MediaDirectories,
    private val validator: BackupValidator = BackupValidator(),
    private val json: Json = Json { prettyPrint = true },
) {
    fun export(outputFile: File): Flow<BackupExportState> = flow {
        emit(BackupExportState.Preparing)
        val tempRoot = File(context.cacheDir, "backup_export_${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        try {
            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            emit(BackupExportState.CopyingDatabase)
            val dbDir = File(tempRoot, BackupConstants.DATABASE_DIR)
            dbDir.mkdirs()
            val dbCopy = File(dbDir, BackupConstants.DATABASE_FILE_NAME)
            databaseProvider.createConsistentDatabaseCopy(dbCopy)

            val mediaToCopy = mediaDirectories.listExportFiles()
            emit(BackupExportState.CopyingMedia(0, mediaToCopy.size))
            val mediaRoot = File(tempRoot, BackupConstants.MEDIA_DIR)
            mediaToCopy.forEachIndexed { index, (relative, source) ->
                if (!source.exists()) return@forEachIndexed
                val target = File(mediaRoot, relative)
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
                emit(BackupExportState.CopyingMedia(index + 1, mediaToCopy.size))
            }

            emit(BackupExportState.WritingManifest)
            val manifest = buildManifest(tempRoot)
            File(tempRoot, BackupConstants.MANIFEST_FILE).writeText(
                json.encodeToString(BackupManifest.serializer(), manifest),
            )

            emit(BackupExportState.Compressing)
            zipDirectory(tempRoot, outputFile)

            emit(BackupExportState.Validating)
            validateArchive(outputFile, manifest)

            emit(BackupExportState.Completed(outputFile))
        } catch (e: Exception) {
            emit(BackupExportState.Failed(e))
        } finally {
            tempRoot.deleteRecursively()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildManifest(tempRoot: File): BackupManifest {
        val files = mutableListOf<BackupManifestFile>()
        var totalBytes = 0L
        tempRoot.walkTopDown().filter { it.isFile && it.name != BackupConstants.MANIFEST_FILE }
            .forEach { file ->
                val relative = tempRoot.toRelativeString(file).replace('\\', '/')
                val mediaType = classifyMediaType(relative)
                val sha = mediaDirectories.sha256(file)
                totalBytes += file.length()
                files.add(
                    BackupManifestFile(
                        relativePath = relative,
                        sizeBytes = file.length(),
                        sha256 = sha,
                        mediaType = mediaType,
                    ),
                )
            }
        val dbRelative = "${BackupConstants.DATABASE_DIR}/${BackupConstants.DATABASE_FILE_NAME}"
        val dbSha = files.first { it.relativePath == dbRelative }.sha256
        return BackupManifest(
            formatName = BackupConstants.FORMAT_NAME,
            formatVersion = BackupConstants.FORMAT_VERSION,
            databaseSchemaVersion = DatabaseConstants.SCHEMA_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = System.currentTimeMillis(),
            databaseFile = dbRelative,
            databaseSha256 = dbSha,
            mediaFileCount = files.count { it.mediaType != BackupMediaType.DATABASE },
            totalUncompressedBytes = totalBytes,
            files = files.sortedBy { it.relativePath },
        )
    }

    private fun classifyMediaType(relativePath: String): BackupMediaType {
        return when {
            relativePath.startsWith("${BackupConstants.DATABASE_DIR}/") -> BackupMediaType.DATABASE
            relativePath.contains("/${MediaDirectories.AUDIO_DIR}/") ||
                relativePath.startsWith("${BackupConstants.MEDIA_DIR}/${MediaDirectories.AUDIO_DIR}") ->
                BackupMediaType.AUDIO
            relativePath.contains("/${MediaDirectories.IMAGES_DIR}/") ||
                relativePath.startsWith("${BackupConstants.MEDIA_DIR}/${MediaDirectories.IMAGES_DIR}") ->
                BackupMediaType.IMAGE
            else -> BackupMediaType.OTHER
        }
    }

    private fun zipDirectory(sourceDir: File, outputZip: File) {
        ZipOutputStream(outputZip.outputStream()).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = sourceDir.toRelativeString(file).replace('\\', '/')
                validator.validateRelativeArchivePath(entryName)
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun validateArchive(archive: File, expectedManifest: BackupManifest) {
        if (archive.length() > BackupConstants.MAX_BACKUP_SIZE_BYTES) {
            throw BackupValidationException.SecurityViolation("Backup archive exceeds max size")
        }
        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence().toList()
            if (entries.size > BackupConstants.MAX_FILE_COUNT) {
                throw BackupValidationException.SecurityViolation("Too many zip entries")
            }
            val manifestEntry = zip.getEntry(BackupConstants.MANIFEST_FILE)
                ?: throw BackupValidationException.MissingFile(BackupConstants.MANIFEST_FILE)
            val manifestText = zip.getInputStream(manifestEntry).bufferedReader().readText()
            val parsed = validator.parseManifest(manifestText)
            validator.validateManifest(parsed)
            if (parsed.databaseSha256 != expectedManifest.databaseSha256) {
                throw BackupValidationException.ChecksumMismatch(parsed.databaseFile)
            }
        }
    }
}

private fun File.toRelativeString(file: File): String {
    return this.canonicalFile.toPath().relativize(file.canonicalFile.toPath()).toString()
}
