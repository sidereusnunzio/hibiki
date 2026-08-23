package com.hibiki.data.media

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class ImageFileStore(private val imagesDir: File) {
    init {
        imagesDir.mkdirs()
    }

    fun createTempFile(): File = File(imagesDir.parentFile, "image_tmp").let { dir ->
        dir.mkdirs()
        File(dir, "tmp_${UUID.randomUUID()}.jpg")
    }

    fun writeJpeg(bitmap: Bitmap, dest: File) {
        dest.parentFile?.mkdirs()
        dest.outputStream().use { output ->
            if (!bitmap.compress(CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw IOException("JPEG compress failed")
            }
        }
    }

    fun persist(tempFile: File): String {
        if (!tempFile.exists()) {
            throw IOException("Temp image missing")
        }
        imagesDir.mkdirs()
        val dest = File(imagesDir, "${UUID.randomUUID()}.jpg")
        tempFile.copyTo(dest, overwrite = false)
        tempFile.delete()
        return dest.absolutePath
    }

    fun installNamed(fileName: String, input: InputStream, overwrite: Boolean = false): String {
        require(fileName.isNotBlank() && fileName == File(fileName).name) {
            "Invalid image file name"
        }
        imagesDir.mkdirs()
        val dest = File(imagesDir, fileName)
        if (overwrite || !dest.isFile) {
            input.use { src ->
                dest.outputStream().use { dst -> src.copyTo(dst) }
            }
        } else {
            input.close()
        }
        return dest.absolutePath
    }

    fun exists(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).isFile

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    companion object {
        private const val JPEG_QUALITY = 90
    }
}
