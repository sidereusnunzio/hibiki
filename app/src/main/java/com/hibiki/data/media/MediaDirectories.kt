package com.hibiki.data.media

import android.content.Context
import java.io.File
import java.security.MessageDigest

class MediaDirectories(context: Context) {
    val audioDir: File = File(context.filesDir, AUDIO_DIR).also { it.mkdirs() }
    val imagesDir: File = File(context.filesDir, IMAGES_DIR).also { it.mkdirs() }

    fun listExportFiles(): List<Pair<String, File>> {
        val files = mutableListOf<Pair<String, File>>()
        audioDir.walkTopDown().filter { it.isFile }.forEach { file ->
            files += "$AUDIO_DIR/${file.name}" to file
        }
        imagesDir.walkTopDown().filter { it.isFile }.forEach { file ->
            files += "$IMAGES_DIR/${file.name}" to file
        }
        return files.sortedBy { it.first }
    }

    fun copyToBackupMedia(destRoot: File) {
        copyDir(audioDir, File(destRoot, AUDIO_DIR))
        copyDir(imagesDir, File(destRoot, IMAGES_DIR))
    }

    fun replaceFromBackupMedia(sourceRoot: File) {
        replaceDir(File(sourceRoot, AUDIO_DIR), audioDir)
        replaceDir(File(sourceRoot, IMAGES_DIR), imagesDir)
    }

    fun clear() {
        if (audioDir.exists()) audioDir.deleteRecursively()
        if (imagesDir.exists()) imagesDir.deleteRecursively()
        audioDir.mkdirs()
        imagesDir.mkdirs()
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun copyDir(source: File, dest: File) {
        if (!source.exists()) return
        dest.mkdirs()
        source.copyRecursively(dest, overwrite = true)
    }

    private fun replaceDir(source: File, dest: File) {
        if (dest.exists()) dest.deleteRecursively()
        if (source.exists()) {
            source.copyRecursively(dest, overwrite = true)
        } else {
            dest.mkdirs()
        }
    }

    companion object {
        const val AUDIO_DIR = "audio"
        const val IMAGES_DIR = "images"
    }
}
