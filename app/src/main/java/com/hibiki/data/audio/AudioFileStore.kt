package com.hibiki.data.audio

import android.content.Context
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import java.io.File
import java.io.IOException
import java.util.UUID

class AudioFileStore(context: Context) {
    private val audioDir = File(context.filesDir, "audio").also { it.mkdirs() }
    private val tempDir = File(context.cacheDir, "audio_tmp").also { it.mkdirs() }

    fun createTempFile(extension: String): File =
        File(tempDir, "tmp_${UUID.randomUUID()}.$extension")

    fun persist(tempFile: File): String {
        if (!tempFile.exists()) {
            throw AppException(AppError.FileSaveFailed)
        }
        val dest = File(audioDir, "${UUID.randomUUID()}.m4a")
        try {
            if (availableBytes() < tempFile.length() + 64_000) {
                throw AppException(AppError.InsufficientStorage)
            }
            tempFile.copyTo(dest, overwrite = false)
        } catch (error: AppException) {
            dest.delete()
            throw error
        } catch (error: IOException) {
            dest.delete()
            if (error.message?.contains("No space", ignoreCase = true) == true) {
                throw AppException(AppError.InsufficientStorage, error)
            }
            throw AppException(AppError.FileSaveFailed, error)
        }
        return dest.absolutePath
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun deleteQuietly(file: File?) {
        runCatching { file?.delete() }
    }

    private fun availableBytes(): Long = audioDir.usableSpace
}
